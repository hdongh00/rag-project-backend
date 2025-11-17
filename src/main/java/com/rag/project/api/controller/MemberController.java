package com.rag.project.api.controller;

import com.rag.project.api.domain.Member;
import com.rag.project.api.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController //이 클래스가 REST API의 컨트롤러
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    //API 위한 DTO
    @RequiredArgsConstructor
    static class SignUpRequest{
        public final String email;
        public final String password;
        public final String username;
    }

    @RequiredArgsConstructor
    static class SignUpResponse {
        public final Long memberId;
        public final String email;
        public final String username;
    }

    //회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request){
        //서비스에게 요청 전달
        Member newMember = memberService.signup(request.email, request.password, request.username);

        //응답 생성
        SignUpResponse response = new SignUpResponse(newMember.getId(), newMember.getEmail(), newMember.getUsername());

        //HTTP 201 Created 상태와 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    //[로그인 API]
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){

        //서비스에 email, password 전달 -> 토큰 받기
        String token = memberService.login(request.email, request.password);

        //토큰을 응답 DTO에 담아서 반환
        return ResponseEntity.ok(new LoginResponse(token));
    }

    //[로그인 요청] DTO
    @RequiredArgsConstructor
    static class LoginRequest{
        public final String email;
        public final String password;
    }

    //[로그인 응답] DTO
    @RequiredArgsConstructor
    static class LoginResponse {
        public final String accessToken;
    }
}
