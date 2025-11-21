package com.rag.project.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    //스프링 ai가 자동으로 주입해주는 임베딩 도구
    private final EmbeddingModel embeddingModel;

    /**
     * 텍스트를 벡터로 변환
     */
    public float[] getEmbedding(String text){
        log.info("Spring AI 임베팅 변환 시작. 텍스트 길이: {}", text.length());

        try{
            //Spring AI에게 텍스트를 주고 임베딩 요청
            float[] vector = embeddingModel.embed(text);

            log.info("임베딩 성공. 벡터 차원: {}", vector.length);
            return vector;
        }catch(Exception e){
            log.error("Spring AI 임베딩 실패: {}", e.getMessage());
            throw new RuntimeException("임베딩 변환 중 오류 발생", e);
        }
    }
}
