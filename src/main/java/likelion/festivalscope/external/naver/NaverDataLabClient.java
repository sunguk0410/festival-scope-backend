package likelion.festivalscope.external.naver;

import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NaverDataLabClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;

    public NaverDataLabClient(
            @Value("${naver.client-id:}") String clientId,
            @Value("${naver.client-secret:}") String clientSecret) {
        this.restClient = RestClient.builder().baseUrl("https://naverapihub.apigw.ntruss.com").build();

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Map<String, List<DataPoint>> search(List<String> keywords, LocalDate startDate, LocalDate endDate) {
        if (clientId.isBlank() || clientSecret.isBlank()
                || clientId.contains("${") || clientSecret.contains("${")) {
            log.warn("Naver DataLab credentials are not resolved: clientIdConfigured={}, clientSecretConfigured={}",
                    configured(clientId), configured(clientSecret));
            throw new AnalysisExecutionException("네이버 데이터랩 인증 정보가 설정되지 않았습니다.");
        }

        log.info("Naver DataLab request: startDate={}, endDate={}, timeUnit=month, keywordCount={}, keywords={}",
                startDate, endDate, keywords.size(), keywords);
        DataLabResponse response;
        try {
            response = restClient.post()
                    .uri("/search-trend/v1/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-NCP-APIGW-API-KEY-ID", clientId)
                    .header("X-NCP-APIGW-API-KEY", clientSecret)
                    .body(new DataLabRequest(
                            startDate.toString(), endDate.toString(), "month",
                            keywords.stream().map(keyword -> new KeywordGroup(keyword, List.of(keyword))).toList()))
                    .exchange((request, httpResponse) -> {
                        int status = httpResponse.getStatusCode().value();
                        String rawBody;
                        try (InputStream input = httpResponse.getBody()) {
                            rawBody = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                        }
                        log.info("Naver DataLab response: httpStatus={}, body={}", status, truncate(rawBody));
                        if (status < 200 || status >= 300) {
                            throw new IllegalStateException("Naver DataLab HTTP " + status + ": " + truncate(rawBody));
                        }
                        return objectMapper.readValue(rawBody, DataLabResponse.class);
                    });
        } catch (Exception exception) {
            log.error("Naver DataLab call failed: startDate={}, endDate={}, keywordCount={}, reason={}",
                    startDate, endDate, keywords.size(), exception.getMessage(), exception);
            throw new AnalysisExecutionException("네이버 데이터랩 API 호출에 실패했습니다.", exception);
        }

        if (response == null || response.results() == null) {
            throw new AnalysisExecutionException("네이버 데이터랩 응답이 비어 있습니다.");
        }

        return response.results().stream().collect(Collectors.toMap(
                result -> result.title().trim().toLowerCase(),
                result -> result.data().stream()
                        .map(data -> new DataPoint(YearMonth.parse(data.period().substring(0, 7)), data.ratio()))
                        .toList(),
                (first, second) -> first));
    }

    private String normalizeCredential(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private boolean configured(String value) {
        return value != null && !value.isBlank() && !value.contains("${");
    }

    private String truncate(String value) {
        if (value == null) return "null";
        return value.length() <= 1000 ? value : value.substring(0, 1000) + "...";
    }

    public record DataPoint(YearMonth month, BigDecimal ratio) {}
    private record DataLabRequest(String startDate, String endDate, String timeUnit, List<KeywordGroup> keywordGroups) {}
    private record KeywordGroup(String groupName, List<String> keywords) {}
    private record DataLabResponse(String startDate, String endDate, String timeUnit, List<Result> results) {}
    private record Result(String title, List<String> keywords, List<RawData> data) {}
    private record RawData(String period, BigDecimal ratio) {}
}