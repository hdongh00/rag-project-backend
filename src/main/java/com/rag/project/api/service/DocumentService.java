package com.rag.project.api.service;

import com.rag.project.api.domain.Document;
import com.rag.project.api.domain.DocumentRepository;
import com.rag.project.api.domain.Member;
import com.rag.project.api.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
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

        //memberEmail로 Member 엔티티를 찾음
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원을 찾을 수 없습니다: " + memberEmail));

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

        //DocumentRepository를 통해 DB저장
        return documentRepository.save(document);
    }
}
