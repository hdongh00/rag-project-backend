package com.rag.project.api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    /**
     * 벡터 유사도 검색
     * 질문 벡터와 가장 거리가 가까운 문서 조각 5개 찾기
     */
    //@query: jpa가 만들어주는 기본기능으로 해결 안 될 때, 직접 SQL을 작성
    //<->: 벡터 간 거리를 계산하는 pgvector 연산자
    @Query(value = "SELECT * FROM document_embeddings " +
                   "ORDER BY embedding_vector <-> cast(:queryVector as vector) ASC " +
                   "LIMIT 5", nativeQuery = true) //가장 유사한 5개 가져옴
    List<DocumentEmbedding> findNearest(@Param("queryVector") float[] queryVector);
}
