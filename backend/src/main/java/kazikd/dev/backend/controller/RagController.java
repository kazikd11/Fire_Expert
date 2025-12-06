package kazikd.dev.backend.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kazikd.dev.backend.model.ChatRequest;
import kazikd.dev.backend.service.ChatService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/query")
public class RagController {

    private final ChatService chatService;

    @Autowired
    public RagController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<kazikd.dev.backend.model.ChatResponse> generateAnswer(@RequestBody ChatRequest request) {
        try {
            log.info("Received question with {} previous messages", 
                    request.getPreviousMessages() != null ? request.getPreviousMessages().size() : 0);
            
            var result = chatService.generateTextResponse(
                    request.getQuestion(), 
                    request.getPreviousMessages() != null ? request.getPreviousMessages() : Collections.emptyList()
            );
            
            return ResponseEntity.ok(new kazikd.dev.backend.model.ChatResponse(
                    result.getAnswer(), 
                    "success", 
                    result.isUsedFullDocuments()
            ));
        } catch (Exception e) {
            log.error("Error generating answer: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new kazikd.dev.backend.model.ChatResponse("Przepraszam, wystąpił błąd. Spróbuj ponownie.", "error", false));
        }
    }
}