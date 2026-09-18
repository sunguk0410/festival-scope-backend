package likelion.festivalscope.trend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.ai.client.OpenAiClient;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendKeywordService {
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> extractKeywords(String festivalName, List<String> programNames) {
        List<String> programs = programNames == null ? List.of() : programNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .toList();
        if (programs.isEmpty()) {
            throw new AnalysisExecutionException("TREND_FIT requires at least one festival program.");
        }

        String input = "festivalName: " + (festivalName == null ? "" : festivalName.trim())
                + "\nprogramNames:\n- " + String.join("\n- ", programs);
        JsonNode root;
        try {
            root = objectMapper.readTree(openAiClient.completeJson(TrendKeywordPrompt.SYSTEM, input));
        } catch (AnalysisExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisExecutionException("Invalid OpenAI trend keyword response.", exception);
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        JsonNode keywordNodes = root.path("keywords");
        if (keywordNodes.isArray()) {
            keywordNodes.forEach(node -> {
                if (node.isTextual() && !node.asText().isBlank()) keywords.add(node.asText().trim());
            });
        }
        if (keywords.isEmpty()) {
            throw new AnalysisExecutionException("OpenAI returned no trend keywords.");
        }
        return new ArrayList<>(keywords).subList(0, Math.min(5, keywords.size()));
    }
}
