package com.rag.project.api.filter;

import com.rag.project.api.service.MemberService;
import com.rag.project.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil; //토큰 생성/검증
    private final MemberService memberService;

    //실제 필터링 로직 수행 되는 곳
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)throws
            ServletException, IOException{

        //"Authorization" 헤더 찾음
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        //헤더가 없어가 "Bearer"로 시작안하면 토큰 없는 것
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        //Bearer 부분을 잘라내고 순수 토큰만 추출
        final String token = authHeader.substring(7);
        //토큰 검증(유효기간, 서명)
        if(!jwtUtil.validateToken(token)){
            filterChain.doFilter(request,response); //유효하지 않으면 다음 필터로
            return;
        }
        //토큰이 유효 -> 토큰에서 이메일을 추출
        final String email = jwtUtil.getEmailToken(token);

        //Spring Security에 이미 인증 정보 있는지 확인
        if(SecurityContextHolder.getContext().getAuthentication() == null){
            //이메일을 이용해 DB에서 사용자 정보 가져옴
            UserDetails userDetails = memberService.loadUserByUsername(email);

            //사용자 정보로 인증 토큰 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails.getUsername(),
                    null, userDetails.getAuthorities());

            //요청 세부 정보를 인증 토큰에 추가
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            //Spring Security에 인증된 사용자 정보 등록
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        filterChain.doFilter(request,response);
    }
}
