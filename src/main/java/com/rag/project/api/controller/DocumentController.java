package com.rag.project.api.controller;

import com.rag.project.api.dto.DocumentUploadResponse;
import com.rag.project.api.domain.Document;
import com.rag.project.api.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(@AuthenticationPrincipal String memberEmail) {
        List<Document> documents = documentService.getMemberDocuments(memberEmail);

        List<DocumentResponse> response = documents.stream()
                .map(doc -> new DocumentResponse(doc.getId(), doc.getOriginalFileName(), doc.getCreateAt()))
                .toList();
        return ResponseEntity.ok(response);
    }
    record DocumentResponse(Long id, String fileName, java.time.LocalDateTime createAt) {}

}
