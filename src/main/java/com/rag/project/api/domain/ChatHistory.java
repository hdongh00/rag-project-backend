package com.rag.project.api.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)//기본 생성자 생성
@EntityListeners(AuditingEntityListener.class)//생성 시간을 자동으로 기록하기 위해 필요
@Table(name = "chat_history")
public class ChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //ID는 1씩 자동 증가
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) //다대일 관계: 여러 채팅 메시지가 한 명의 회원에게 속함
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String role; //말한 사람

    @Column(columnDefinition = "TEXT", nullable = false) // 긴 대화 내용도 저장
    private String content;

    @CreatedDate //데이터가 생성될 때 시간을 자동으로 기록
    private LocalDateTime createdAt;

    @Builder //객체 생성 편하게 해주는 빌더 패턴
    public ChatHistory(Member member, String role, String content) {
        this.member = member;
        this.role = role;
        this.content = content;
    }
}
