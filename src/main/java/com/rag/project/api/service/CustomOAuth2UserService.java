package com.rag.project.api.service;

import com.rag.project.api.domain.Member;
import com.rag.project.api.domain.MemberRepository;
import com.rag.project.api.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        //구글에서 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("구글 로그인 정보 가져오기 성공: {}", oAuth2User.getAttributes());

        //정보 추출(구글은 'sub'가 고유 ID, 'email', 'name' 등을 줌)
        String provider = userRequest.getClientRegistration().getRegistrationId(); //google
        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        //우리 DB에 저장하거나 업데이트
        Member member = memberRepository.findByEmail(email)
                .map(entity -> entity.update(name)) //이미 있으면 이름만 업데이트
                .orElse(Member.builder()
                        .email(email)
                        .username(name)
                        .password(null) //소셜 로그인은 비번 X
                        .role(Role.USER)
                        .provider(provider)
                        .providerId(providerId)
                        .build());

        memberRepository.save(member);

        return oAuth2User;
    }
}
