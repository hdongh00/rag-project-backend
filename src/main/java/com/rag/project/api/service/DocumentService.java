package com.rag.project.api.service;

import com.rag.project.api.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

// Apache POI (Word, PPT) 관련 import
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j //로그 찍기 위한 롬복
@Service
@RequiredArgsConstructor
public class DocumentService {

    //S3 통신 동구
    private final S3Client s3Client;
    //테이블 관리자(Document)
    private final DocumentRepository documentRepository;
    //테이블 관리자(Member)
    private final MemberRepository memberRepository;
    private final EmbeddingService embeddingService;          // 벡터 변환기
    private final DocumentEmbeddingRepository embeddingRepository; // 벡터 DB 관리자

    //yml에 등록한 S3 버킷 이름
    @Value("${aws.s3.bucket}")
    private String bucket;
    /**
     * 파일을 S3에 업로드하고, 그 메타데이터를 DB에 저장합니다.
     * @param file 업로드할 파일
     * @param memberEmail 업로드한 회원 (JWT 토큰에서 추출한)
     * @return DB에 저장된 Document 객체
     * @throws IOException
     */
    @Transactional
    public Document uploadDocument(MultipartFile file, String memberEmail) throws IOException{

        //memberEmail로 Member 엔티티를 찾음(회원 조회)
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원을 찾을 수 없습니다: " + memberEmail));

        //텍스트 추출
        String extractedText = extractText(file);
        log.info("파일 텍스트 추출 완료! 길이: {} 자", extractedText.length());
        //log.info("추출된 텍스트일부(앞 100자): {}", extractedText.substring(0, Math.min(extractedText.length(), 100)));
        //s3에 저장할 파일 이름 생성(중복 방지)
        String originalFileName = file.getOriginalFilename();
        String s3FileName = UUID.randomUUID().toString() + "-" + originalFileName;

        //S3에 올릴 파일에 대한 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3FileName)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        //s3에 파일 업로드
        log.info("S3에 파일 업로드 시작: {}", s3FileName);
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("S3에 파일 업로드 완료");

        //S3에 저장된 파일의 Url 가져오기
        String s3FileUrl = s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucket)
                .key(s3FileName)
                .build()).toString();

        //DB에 저장할 Document 엔티티 생성
        Document document = Document.builder()
                .originalFileName(originalFileName)
                .s3FileUrl(s3FileUrl).member(member)
                .build();

        //DB 저장 후, 변수에 담기, 바로 return X, 변수에 담아둠
        Document savedDocument = documentRepository.save(document);

        //RAG 파이프라인: 텍스트 쪼개기 & 벡터 저장
        if(extractedText != null && !extractedText.isBlank()){
            //텍스트 쪼개기
            List<String> chunks = splitTextIntoChunks(extractedText, 500);

            for(String chunk : chunks){
                //각 조각을 벡터로 변환
                float[] vector = embeddingService.getEmbedding(chunk);

                //DocumentEmbedding 엔티티 생성 및 저장
                DocumentEmbedding embeddingEntity = DocumentEmbedding.builder()
                        .document(savedDocument)
                        .textSegment(chunk)
                        .embeddingVector(vector).build();

                embeddingRepository.save(embeddingEntity);
            }
            log.info("벡터 데이터 {}개 저장 완료!", chunks.size());
        }

        return savedDocument; //최종 변환
    }

    /**
     * 파일에서 텍스트를 추출
     */
    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename().toLowerCase();

        //pdf 파일
        if(contentType.equals("application/pdf") || fileName.endsWith(".pdf")){
            try(PDDocument document = PDDocument.load(file.getInputStream())){
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }

        //텍스트 파일
        else if(contentType.startsWith("text/") || fileName.endsWith(".txt")){
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        //Word 파일
        else if(fileName.endsWith(".docx")){
            return extractFromWord(file);
        }

        //PPT 파일
        else if(fileName.endsWith(".pptx")){
            return extractFromPpt(file);
        }
        else{
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + contentType);
        }

    }

    //Word 파일 텍스트 추출
    private String extractFromWord(MultipartFile file) throws IOException{
        try(XWPFDocument doc = new XWPFDocument(file.getInputStream());
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc)){
            return extractor.getText();
        }
    }

    // PPT 파일 텍스트 추출
    private String extractFromPpt(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();

        // XMLSlideShow: .pptx 파일을 읽는 POI 클래스
        try (XMLSlideShow ppt = new XMLSlideShow(file.getInputStream())) {
            // 슬라이드 하나씩 순회
            for (XSLFSlide slide : ppt.getSlides()) {
                // 슬라이드 안에 있는 도형(Shape)들 중에서 '글 상자'만 찾음
                slide.getShapes().stream()
                        .filter(shape -> shape instanceof XSLFTextShape) // 텍스트가 있는 도형인가?
                        .map(shape -> (XSLFTextShape) shape)             // 형변환
                        .forEach(textShape -> sb.append(textShape.getText()).append("\n")); // 텍스트 꺼내서 합치기
            }
        }
        return sb.toString();
    }

    //텍스트 분할 메서드
    private List<String> splitTextIntoChunks(String text, int chunkSize){
        List<String> chunks = new ArrayList<>();
        for(int i = 0; i < text.length(); i += chunkSize){
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }
    @Transactional(readOnly = true)
    public List<Document> getMemberDocuments(String memberEmail){
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        return documentRepository.findByMemberId(member.getId());
    }

    @Transactional
    public void deleteDocument(Long documentId, String memberEmail){
        //문서 조회
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서가 존재하지 않습니다."));

        //권한 확인
        if(!document.getMember().getEmail().equals(memberEmail)){
            throw new IllegalArgumentException("이 문서를 삭제할 권한이 없습니다.");
        }

        //s3에서 파일 삭제
        String fileKey = document.getS3FileUrl().substring(document.getS3FileUrl().lastIndexOf("/") + 1);

        try{
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S# 파일 삭제 완료: {}", fileKey);
        }catch(Exception e){
            // s3에 파일 없어도 DB 삭제는 진행하기 위해 로그만 찍음
            log.error("S3 파일 삭제 실패 (DB 삭제는 진행): {}", e.getMessage());
        }
        //DB에서 문서 정보 삭제
        documentRepository.delete(document);
    }
}
