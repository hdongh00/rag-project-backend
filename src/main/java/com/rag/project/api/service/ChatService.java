package com.rag.project.api.service;

import com.rag.project.api.domain.DocumentEmbedding;
import com.rag.project.api.domain.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; //로그 기능 추가
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

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

    //프롬프트 템플릿
    private static final String RAG_PROMPT_TEMPLATE = """
            당신은 사용자가 업로드한 문서를 기반으로 답변해주는 AI 어시스턴트입니다.
            아래 제공된 [문서 내용]을 바탕으로 [사용자 질문]에 대해 답변해 주세요.
            만약 문서에 없는 내용이라면 "문서에 해당 내용이 없습니다"라고 솔직하게 말해 주세요.
            
            [문서 내용]
            {context}
            
            [사용자 질문]
            {question}
            """;

    public String chat(String question){
        //사용자 질문을 벡터로 변환
        float[] questionVector = embeddingService.getEmbedding(question);

        //DB에서 질문과 가장 유사한 문서 조각 5개 검색
        List<DocumentEmbedding> similarChunks = embeddingRepository.findNearest(questionVector);

        //검색된 문서 조각들의 텍스트만 뽑아서 하나로 합침
        String context = similarChunks.stream()
                .map(DocumentEmbedding::getTextSegment)
                .collect(Collectors.joining("\n\n"));

        log.info("검색된 관련 문서 조각 개수: {}", similarChunks.size());
        log.info("참조한 문서 내용: {}", context); // (디버깅용)

        //프롬프트 템플릿에 문서 내용과 질문 채워 넣기
        PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", question
        ));

        //GPT에게 최종 질문하고 답변
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}
