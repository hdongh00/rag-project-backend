package com.rag.project.api.service;

import com.rag.project.api.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; //로그 기능 추가
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final EmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final ChatModel chatModel; //GPT에게 질문 (Spring AI가 자동 주입)

    private final ChatHistoryRepository chatHistoryRepository; // 대화 기록 저장소
    private final MemberRepository memberRepository; // 회원 정보 조회용

    //프롬프트 템플릿
    private static final String RAG_PROMPT_TEMPLATE = """
            당신은 사용자가 업로드한 문서를 기반으로 답변해주는 AI 어시스턴트입니다.
            
            [이전 대화 기록]
            {history}
            
            [문서 내용]
            {context}
            
            [사용자 질문]
            {question}
            
            위의 [문서 내용]을 최우선으로 참고하여 답변하세요.\s
            문서에 없는 내용은 [이전 대화 기록]을 참고하거나 "문서에 해당 내용이 없습니다"라고 말하세요.
            """;

    @Transactional
    public String chat(String question, String memberEmail){

        //회원 정보 조회
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        //사용자 질문을 벡터로 변환
        float[] questionVector = embeddingService.getEmbedding(question);

        //DB에서 질문과 가장 유사한 문서 조각 5개 검색
        List<DocumentEmbedding> similarChunks = embeddingRepository.findNearest(questionVector);

        //검색된 문서 조각들의 텍스트만 뽑아서 하나로 합침
        String context = similarChunks.stream()
                .map(DocumentEmbedding::getTextSegment)
                .collect(Collectors.joining("\n\n"));

        //이전 대화 기록 가져오기
        //최근 10개 가져와서, 시간 역순으로 정렬하고 문자열로 합침
        List<ChatHistory> histories = chatHistoryRepository.findRecentChats(member.getId());
        Collections.reverse(histories); //최신순으로 가져왔으니 다시 과거순으로 뒤집음

        String historyText = histories.stream()
                        .map(h -> h.getRole() + ": " + h.getContent())
                                .collect(Collectors.joining("\n"));

        //프롬프트 생성
        PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", question,
                "history", historyText
        ));

        //AI 답변 받기
        String answer = chatModel.call(prompt).getResult().getOutput().getContent();

        //이번 대화를 DB에 저장
        chatHistoryRepository.save(ChatHistory.builder()
                .member(member)
                .role("user")
                .content(question)
                .build());

        //AI 답변 저장
        chatHistoryRepository.save(ChatHistory.builder()
                .member(member)
                .role("assistant")
                .content(answer)
                .build());

        return answer;
        //log.info("검색된 관련 문서 조각 개수: {}", similarChunks.size());
        //log.info("참조한 문서 내용: {}", context); // (디버깅용)



        //GPT에게 최종 질문하고 답변
        //return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}
