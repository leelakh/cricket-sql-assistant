
package dev.leelakh.cricket_sql.rest;

import dev.leelakh.cricket_sql.service.CricketAiQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cricket")
public class CricketAiController {

    private final CricketAiQueryService cricketAiQueryService;

    public CricketAiController(
            CricketAiQueryService cricketAiQueryService
    ) {
        this.cricketAiQueryService = cricketAiQueryService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(
            @RequestBody Map<String, String> request
    ) {
        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Question cannot be empty")
            );
        }

        List<Map<String, Object>> results =
                cricketAiQueryService.ask(question);

        return ResponseEntity.ok(
                Map.of(
                        "question", question,
                        "results", results
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", exception.getMessage())
        );
    }
}
