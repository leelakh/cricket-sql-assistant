
package dev.leelakh.cricket_sql.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private final RestClient restClient;

    public OllamaService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String ask(String question) {

        Map<String, Object> request = Map.of(
                "model", "qwen2.5:3b",
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", question
                        )
                ),
                "stream", false
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "No response received from Ollama"
            );
        }

        Map<?, ?> message = (Map<?, ?>) response.get("message");

        if (message == null) {
            throw new IllegalStateException(
                    "Ollama response did not contain a message"
            );
        }

        return (String) message.get("content");
    }
}