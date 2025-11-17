package com.rag.project.api.controller;

import com.rag.project.api.domain.Document;
import com.rag.project.api.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    //문서 업로드 API
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) //파일을 받음
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal String memberEmail
            ) throws IOException {
        Document savedDocument = documentService.uploadDocument(file, memberEmail);

        //응답 생성
        DocumentUploadResponse response = new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getOriginalFileName(),
                savedDocument.getS3FileUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //DTO
    @RequiredArgsConstructor
    static class DocumentUploadResponse {
        public final Long documentId;
        public final String originalFileName;
        public final String s3FileUrl;
    }
}
