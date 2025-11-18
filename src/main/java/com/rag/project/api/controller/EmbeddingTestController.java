package com.rag.project.api.controller;

import com.rag.project.api.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final EmbeddingService embeddingService;

    // 테스트용 API: http://localhost:8080/test/embedding?text=안녕
    @GetMapping("/test/embedding")
    public Map<String, Object> testEmbedding(@RequestParam(name = "text", defaultValue = "Hello Spring AI") String text) {

        // 1. 서비스 호출 (텍스트 -> 벡터 변환)
        float[] vector = embeddingService.getEmbedding(text);

        // 2. 결과 확인 (JSON으로 반환)
        return Map.of(
                "inputText", text,
                "vectorSize", vector.length, // 벡터의 차원 수 (1536이어야 함)
                "vectorSample", vector // 전체 벡터 데이터
        );
    }
}