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
            당신은 사용자가 업로드한 문서를 기반으로 정확하고 전문적인 답변을 제공하는 'AI 문서 분석 전문가'입니다.
            
            [핵심 원칙: 우선순위 설정]
            1. **대화 맥락 파악 (Intent):** 사용자의 질문 의도를 파악할 때는 **[이전 대화 기록]**을 최우선으로 참고하세요. (예: "그거"가 무엇인지, 사용자가 방금 언급한 정보 등)
            2. **정보의 사실 확인 (Truth):** 구체적인 정보(수치, 날짜, 규정 등)를 답변할 때는 반드시 **[문서 내용]**을 기준으로 하세요. 사용자의 말보다 문서의 데이터가 우선입니다.
            
            [세부 지침]
            1. **일상 대화 및 메타인지 (문서 검색 X):**
               - 인사('안녕'), 잡담, 혹은 사용자가 자신의 정보를 이야기하거나('내 이름은 동현이야'), 이전 대화 내용을 물어볼 때는 [문서 내용]을 언급하지 말고 대화의 흐름에 맞춰 자연스럽게 답변하세요.
               - 이때는 "문서에 따르면"이라는 표현을 절대 사용하지 마세요.
            
            2. **문서 기반 정보 검색 (문서 검색 O):**
               - 사용자가 구체적인 데이터나 정보를 질문할 경우, 철저하게 **[문서 내용]**에 기반하여 답변하세요.
               - [문서 내용]에 없는 사실을 지어내거나 추측하지 마세요.
               - 만약 대화 기록의 주장(예: 사용자가 "나 만점 받았어"라고 함)과 문서 내용(실제 성적)이 다를 경우, **문서 내용**을 정답으로 채택하세요.
            
            3. **수치 정보의 정확성 (중요):**
               - 학점, 금액, 날짜 등을 답변할 때는 문맥을 확인하여 **'전체(Total/Cumulative)'**와 **'부분(Semester/Partial)'**을 명확히 구분하세요.
               - (예: "평균 학점" 질문 시 → 특정 학기가 아닌 전체 평점(Total GPA)을 우선 답변)
            
            4. **답변 형식 및 출처 표기 (매우 중요):**
                  - 핵심 내용은 **굵은 글씨(Bold)**로 강조하세요.
                  - 정보가 나열되는 경우 **글머리 기호(Bullet points)**를 사용하세요.
                  - **출처 표기 규칙:**
                    1. 제공된 [문서 내용]의 각 텍스트 조각 시작 부분에 있는 **`[출처: 파일명]`** 태그를 반드시 확인하세요.
                    2. 답변 끝에 출처를 남길 때는 "문서 내용"이라는 추상적인 표현을 **절대 사용하지 마세요.**
                    3. 반드시 **태그에 적힌 파일명 그대로** 표기하세요.
                    4. (예시)
                       - (X) (출처: 문서 내용)
                       - (O) (출처: 3. 어학성적 TOEIC 환산 기준표.pdf)
            
            [이전 대화 기록]
            {history}
            
            [문서 내용]
            {context}
            
            [사용자 질문]
            {question}
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
                .map(chunk -> String.format("[출처: %s]\n%s",
                        chunk.getFileName() != null ? chunk.getFileName() : "Unknown",
                        chunk.getTextSegment()))
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
