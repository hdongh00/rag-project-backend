package com.rag.project.api.dto;

public record SignUpResponse(
        Long memberId,
        String email,
        String username
) {}
