package com.rag.project.api.service;

import com.rag.project.api.domain.Member;
import com.rag.project.api.domain.MemberRepository;
import com.rag.project.api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public Member signup(String email, String password, String username){
        //비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        //Member 엔티티 생성
        Member newMember = Member.builder()
                .email(email)
                .password(encodedPassword)
                .username(username)
                .build();

        return memberRepository.save(newMember);
    }

    //로그인 메서드
    @Transactional(readOnly = true) //읽기 전용 (조회 속도 향상)
    public String login(String email, String password){
        //이메일로 회원 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        //비밀번호 일치 여부 확인
        if(!passwordEncoder.matches(password, member.getPassword())){
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }
        //비밀번호 일치 -> JWT 토큰 생성 및 반환
        return jwtUtil.createToken(member.getEmail());
    }
    /*
    Spring Security가 email(username)을 기반으로 사용자의 인증 정보 로드
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 찾을 수 없습니다: "   + email));

        //Spring Security는 비밀번호가 null이면 에러, 임시로 빈 문자열을 추가
        String password = member.getPassword() != null ? member.getPassword() : "";

        return User.builder()
                .username(member.getEmail())
                .password(password) //
                .roles(member.getRole().name())
                .build();
    }
}
