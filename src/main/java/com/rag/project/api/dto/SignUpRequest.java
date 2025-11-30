package com.rag.project.api.dto;

public record SignUpRequest(
        String email,
        String password,
        String username
) {}
