package com.rag.project.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id;

    @Column(nullable = false)
    private String originalFileName; //사용자가 업도르한 원본 파일 이름

    @Column(nullable = false)
    private String s3FileUrl; //s3에 저장된 파일의 Url

    @Column(nullable = false, updatable = false)
    private LocalDateTime createAt; //업로드 날짜

    @ManyToOne(fetch = FetchType.LAZY) //지연 로딩
    @JoinColumn(name = "member_id", nullable = false) //FK
    private Member member; //문서를 업로드한 회원

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentEmbedding> embeddings = new ArrayList<>();

    @PrePersist //DB에 Insert 되기 직전에 자동으로 호출
    protected void onCreate() {
        this.createAt = LocalDateTime.now();
    }

    @Builder
    public Document(String originalFileName, String s3FileUrl, Member member) {
        this.originalFileName = originalFileName;
        this.s3FileUrl = s3FileUrl;
        this.member = member;
    }

}
