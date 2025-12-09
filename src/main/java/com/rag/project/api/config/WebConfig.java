//package com.rag.project.api.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") //모든 api 경로
//                .allowedOrigins("http://localhost:5173") //리액트 주소
//                .allowedMethods("GET", "POST", "PUT", "DELETE") //허용한 Http 메소드
//                .allowedHeaders("*") //모든 헤더 허용
//                .allowCredentials(true); //쿠키/인증 정보 포함 허용
//    }
//}
