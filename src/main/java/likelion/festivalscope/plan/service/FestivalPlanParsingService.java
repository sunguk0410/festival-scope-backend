package likelion.festivalscope.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.external.ai.OpenAiClient;
import likelion.festivalscope.document.service.DocumentTextExtractionService;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;
import likelion.festivalscope.plan.dto.response.FestivalPlanParsedResponse;
import likelion.festivalscope.plan.dto.response.ParsedThemeDto;
import likelion.festivalscope.plan.entity.FestivalStatus;
import likelion.festivalscope.plan.entity.VenueType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalPlanParsingService {
    private final DocumentTextExtractionService documentTextExtractionService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${document.parsing.max-text-length:120000}")
    private int maxTextLength;

    public FestivalPlanParsedResponse parse(MultipartFile file) {
        String text = documentTextExtractionService.extract(file);
        if (text.length() > maxTextLength) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "PDF 텍스트가 너무 깁니다. 핵심 내용 중심의 PDF로 다시 업로드해주세요.");
        }
        try {
            JsonNode root = objectMapper.readTree(openAiClient.completeJson(
                    FestivalPlanParsingPrompt.SYSTEM, text));
            return toResponse(root);
        } catch (BusinessException | AnalysisExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisExecutionException("Festival plan parsing response could not be processed.", exception);
        }
    }

    private FestivalPlanParsedResponse toResponse(JsonNode root) {
        return new FestivalPlanParsedResponse(
                text(root, "planName"), text(root, "festivalName"), enumValue(root, "festivalStatus", FestivalStatus.class),
                integer(root, "firstHeldYear"), text(root, "sido"), text(root, "sigungu"), text(root, "venueName"),
                text(root, "venueAddress"), decimal(root, "latitude"), decimal(root, "longitude"), date(root, "startDate"),
                date(root, "endDate"), longValue(root, "targetVisitorCount"), enumValue(root, "venueType", VenueType.class),
                integer(root, "capacity"), themes(root), programs(root));
    }

    private List<ParsedThemeDto> themes(JsonNode root) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        JsonNode nodes = root.path("themeCodes");
        if (nodes.isArray()) nodes.forEach(node -> { if (node.isTextual()) codes.add(node.asText().trim()); });
        return codes.stream().map(FestivalThemeCatalog::toParsedTheme).filter(java.util.Objects::nonNull)
                .map(theme -> new ParsedThemeDto(theme.category(), theme.code(), theme.name())).toList();
    }

    private List<String> programs(JsonNode root) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        JsonNode nodes = root.path("coreProgramNames");
        if (!nodes.isArray()) nodes = root.path("programNames");
        if (nodes.isArray()) nodes.forEach(node -> { if (node.isTextual() && !node.asText().isBlank()) values.add(node.asText().trim()); });
        return new ArrayList<>(values);
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isMissingNode() || node.isNull() || node.asText().isBlank() ? null : node.asText().trim();
    }

    private Integer integer(JsonNode root, String field) {
        Long value = number(root, field);
        return value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? null : value.intValue();
    }

    private Long longValue(JsonNode root, String field) { return number(root, field); }

    private Long number(JsonNode root, String field) {
        String value = text(root, field);
        if (value == null) return null;
        try {
            String normalized = value.replace(",", "").replace("명", "").trim();
            if (normalized.contains("만") || normalized.contains("천")) {
                long result = 0;
                String[] tenThousands = normalized.split("만", 2);
                if (tenThousands.length == 2) {
                    result += Long.parseLong(digits(tenThousands[0], "0")) * 10_000;
                    normalized = tenThousands[1];
                }
                String[] thousands = normalized.split("천", 2);
                if (thousands.length == 2) {
                    result += Long.parseLong(digits(thousands[0], "0")) * 1_000;
                    normalized = thousands[1];
                }
                String remainder = digits(normalized, "0");
                return result + Long.parseLong(remainder);
            }
            return Long.parseLong(digits(normalized, ""));
        }
        catch (NumberFormatException exception) { return null; }
    }

    private String digits(String value, String fallback) {
        String result = value.replaceAll("[^0-9-]", "");
        return result.isBlank() ? fallback : result;
    }

    private BigDecimal decimal(JsonNode root, String field) {
        String value = text(root, field);
        if (value == null) return null;
        try { return new BigDecimal(value.replaceAll("[^0-9.-]", "")); }
        catch (NumberFormatException exception) { return null; }
    }

    private LocalDate date(JsonNode root, String field) {
        String value = text(root, field);
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (Exception exception) { return null; }
    }

    private <E extends Enum<E>> E enumValue(JsonNode root, String field, Class<E> type) {
        String value = text(root, field);
        if (value == null) return null;
        try { return Enum.valueOf(type, value.toUpperCase()); }
        catch (IllegalArgumentException exception) { return null; }
    }
}
