package com.rag.project.api.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    //이메일로 회원을 찾는 기능
    //@parm email 찾을 이메일
    //@return Optional<Member> (회원이 있을수도, 없을수도 있음)
    Optional<Member> findByEmail(String email);
}
