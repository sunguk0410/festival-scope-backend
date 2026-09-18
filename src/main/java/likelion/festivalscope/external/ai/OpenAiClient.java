package likelion.festivalscope.external.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {
    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.model:gpt-4o-mini}") String model,
                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public String completeJson(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank() || apiKey.contains("${")) {
            throw new AnalysisExecutionException("OpenAI API key is not configured.");
        }
        try {
            String rawResponse = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "model", model,
                            "temperature", 0.1,
                            "response_format", Map.of("type", "json_object"),
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt))))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content.isBlank()) throw new IllegalStateException("OpenAI response content is empty.");
            return content;
        } catch (AnalysisExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("OpenAI request failed", exception);
            throw new AnalysisExecutionException("OpenAI API call failed.", exception);
        }
    }
}
