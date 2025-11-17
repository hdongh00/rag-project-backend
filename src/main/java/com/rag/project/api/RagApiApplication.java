package com.rag.project.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// --- [ 2. @OpenAPIDefinition 추가 ] ---
// Swagger UI에 '보안 요구 사항' (BearerAuth)을 전역으로 설정
@OpenAPIDefinition(
        info = @Info(title = "RAG Project API", version = "v1"),
        security = @SecurityRequirement(name = "BearerAuth")
)
// --- [ 3. @SecurityScheme 추가 ] ---
// "BearerAuth"라는 이름의 보안 스킴을 정의
// (HTTP Bearer 토큰 방식을 사용)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SpringBootApplication
public class RagApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApiApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //BCrypt 암호화 사용
    }
}
