package com.rag.project.api.component;

import org.springframework.ai.document.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FileHandler {

    //스마트 청킹, 800토큰 = 한글 400~500자 / 영문 3000자
    private static final int DEFAULT_CHUNK_SIZE = 800;
    //오버랩: 문맥 단절을 막기 위해 앞뒤 내용을 100토큰씩 겹치게
    private static final int DEFAULT_CHUNK_OVERLAP = 100;
    /**
     * 파일에서 텍스트를 추출
     */
    public String extractText(MultipartFile file) throws IOException {
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

    /**
     * SPRING AI TokenTextSplitter를 이용한 스마트 청킹
     * - 단순 글자 수가 아닌 '토큰' 단위로 자름
     * - 문맥 유지를 위해 오버랩 적용
     */
    //텍스트 분할 메서드
    public List<String> splitTextIntoChunks(String text){
        if(text == null || text.isEmpty()){
            return new ArrayList<>();
        }
        // TokenTextSplitter 생성
        TokenTextSplitter splitter = new TokenTextSplitter(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, 5, 10000, true);

        //텍스트를 document 리스트로 분할
        List<Document> documents = splitter.apply(List.of(new Document(text)));

        //document 객체에서 다시 텍스트만 추출, 리스트로 반환
        return documents.stream()
                .map(Document::getContent)
                .collect(Collectors.toList());
    }

    //텍스트 정제 함수
    public String sanitizeText(String text){
        if (text == null){
            return null;
        }
        //0x00를 빈 문자열로 제거
        return text.replace("\u0000", "");
    }
}
