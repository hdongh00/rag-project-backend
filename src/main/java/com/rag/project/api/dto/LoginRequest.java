package com.rag.project.api.dto;

public record LoginRequest(
        String email,
        String password
) {}
