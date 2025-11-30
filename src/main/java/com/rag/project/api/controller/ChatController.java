package com.rag.project.api.controller;

import com.rag.project.api.dto.ChatResponse;
import com.rag.project.api.dto.ChatRequest;
import com.rag.project.api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request
                                             ,@AuthenticationPrincipal String memberEmail){
        //서비스 호출
        String answer = chatService.chat(request.question(), memberEmail);

        //응답
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
