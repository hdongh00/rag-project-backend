package com.rag.project.api.dto;

public record DocumentUploadResponse(
        Long documentId,
        String originalFileName,
        String s3FileUrl
) {}
