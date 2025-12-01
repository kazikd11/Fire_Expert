package kazikd.dev.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kazikd.dev.backend.service.RagService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/query")
public class RagController {

    private final RagService ragService;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping
    public ResponseEntity<String> generateAnswer(@RequestParam String question) {

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body("Brak pytania.");
        }
        try {
            String answer = ragService.generateTextResponse(question);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Błąd podczas generowania odpowiedzi: " + e.getMessage());
        }
    }
}