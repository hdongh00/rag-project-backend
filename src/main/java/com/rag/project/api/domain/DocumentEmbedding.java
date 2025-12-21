package com.rag.project.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "document_embeddings")
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "embedding_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document; //어떤 문서의 조각인지 파악

    @Column(columnDefinition = "TEXT") //긴 텍스트 저장
    private String textSegment; //쪼개진 텍스트 조각

    //벡터 데이터
    //PostgreSQL의 vector 타입과 매핑
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536) //OpenAi 임베딩 차원 수
    @Column(name = "embedding_vector")
    private float[] embeddingVector;

    @Builder
    public DocumentEmbedding(Document document, String textSegment, float[] embeddingVector) {
        this.document = document;
        this.textSegment = textSegment;
        this.embeddingVector = embeddingVector;
    }
    public String getFileName(){
        return this.document != null ? this.document.getFileName() : null;
    }
}
