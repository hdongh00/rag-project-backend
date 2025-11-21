package com.rag.project.api.controller;

import com.rag.project.api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    //질문 요청 DTO
    record ChatRequest(String question){
    }
    //답변 요청 DTO
    record ChatResponse(String answer){
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request){
        //서비스 호출
        String answer = chatService.chat(request.question);

        //응답
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
