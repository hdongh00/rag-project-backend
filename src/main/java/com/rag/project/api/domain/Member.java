package com.rag.project.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA는 기본 생성자 필요
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column
    private String password;

    @Column(nullable = false, length = 30)
    private String username;

    private String provider;
    private String providerId;

    //[추가] 권한 (DB에는 "USER" 같은 글자로 저장)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public Member(String email, String password, String username, String provider, String providerId, Role role) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.provider = provider;
        this.providerId = providerId;
        // 역할이 안 들어오면 기본값으로 'USER' 부여
        this.role = (role != null) ? role : Role.USER;
    }

    //소셜 로그인 시 이름 없데이트 등을 위한 메서드 (필요시 사용)
    public Member update(String username){
        this.username = username;
        return this;
    }
}
