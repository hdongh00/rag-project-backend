package com.rag.project.api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory,Long> {

    @Query(value = "SELECT * FROM chat_history " +
            "WHERE member_id = :memberId " +
            "ORDER BY created_at DESC " +
            "LIMIT 10",
            nativeQuery = true)
    //특정 회원의 대화 기록을 최신순으로 10개 가져옴
    List<ChatHistory> findRecentChats(@Param("memberId") Long memberId);
}
