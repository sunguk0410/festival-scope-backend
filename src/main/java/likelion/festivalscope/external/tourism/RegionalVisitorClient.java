package likelion.festivalscope.external.tourism;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RegionalVisitorClient {
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String key;
    private final String endpoint;

    public RegionalVisitorClient(
            @Value("${tourism.visitor.base-url:https://apis.data.go.kr/B551011/DataLabService}") String baseUrl,
            @Value("${tourism.visitor.service-key:}") String key,
            @Value("${tourism.visitor.endpoint:/locgoRegnVisitrDDList}") String endpoint) {
        this.client = RestClient.builder().build();
        this.baseUrl = baseUrl;
        this.key = normalizeServiceKey(key);
        this.endpoint = endpoint;
    }

    private String normalizeServiceKey(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    public List<VisitorRecord> fetchAll(LocalDate from, LocalDate to) {
        if (key.isBlank()) {
            throw new AnalysisExecutionException("관광수요 방문자 API 인증키가 설정되지 않았습니다.");
        }

        List<Raw> all = new ArrayList<>();
        int page = 1;
        int total = Integer.MAX_VALUE;
        while ((page - 1) * 10_000 < total) {
            int pageNo = page;
            String requestUrl = baseUrl + endpoint
                    + "?serviceKey=" + key
                    + "&numOfRows=10000&pageNo=" + pageNo
                    + "&MobileOS=ETC&MobileApp=FestivalScope"
                    + "&startYmd=" + from.format(DATE)
                    + "&endYmd=" + to.format(DATE)
                    + "&_type=json";
            // serviceKey는 공공데이터포털에서 받은 Encoding 키를 그대로 전달한다.
            URI requestUri = URI.create(requestUrl);
            String requestSummary = requestSummary("NATIONWIDE", from, to, pageNo);
            long startedAt = System.nanoTime();
            log.info("Regional visitor API request started: {}, uri={}",
                    requestSummary, redactServiceKey(requestUri.toString()));

            ApiResponse apiResponse;
            try {
                apiResponse = client.get().uri(requestUri).exchange((request, response) -> {
                    HttpHeaders headers = response.getHeaders();
                    return new ApiResponse(
                            response.getStatusCode().value(),
                            headers.getFirst(HttpHeaders.CONTENT_TYPE),
                            headers.getFirst(HttpHeaders.CONTENT_LENGTH),
                            response.bodyTo(String.class));
                });
            } catch (Exception e) {
                log.error("Regional visitor API request failed: {}, elapsedMs={}, exceptionType={}, message={}",
                        requestSummary, elapsedMs(startedAt), e.getClass().getName(), e.getMessage(), e);
                throw new AnalysisExecutionException("관광수요 방문자 API 호출에 실패했습니다.", e);
            }

            String rawBody = apiResponse.body();
            log.info("Regional visitor API response received: {}, httpStatus={}, contentType={}, "
                            + "contentLengthHeader={}, bodyLength={}, elapsedMs={}",
                    requestSummary, apiResponse.status(), apiResponse.contentType(),
                    apiResponse.contentLength(), lengthOf(rawBody), elapsedMs(startedAt));

            JsonNode root;
            try {
                root = rawBody == null || rawBody.isBlank() ? null : objectMapper.readTree(rawBody);
            } catch (Exception e) {
                log.error("Regional visitor API response JSON parse failed: {}, httpStatus={}, "
                                + "contentType={}, bodyLength={}, bodyPreview={}",
                        requestSummary, apiResponse.status(), apiResponse.contentType(),
                        lengthOf(rawBody), truncate(rawBody), e);
                throw new AnalysisExecutionException("관광수요 방문자 API 응답을 해석할 수 없습니다.", e);
            }

            JsonNode openApiError = root == null ? null
                    : root.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
            if (openApiError != null && openApiError.isObject()) {
                String errorMessage = textOrNull(openApiError, "errMsg");
                String authMessage = textOrNull(openApiError, "returnAuthMsg");
                String reasonCode = textOrNull(openApiError, "returnReasonCode");
                log.error("Regional visitor API gateway rejected request: {}, httpStatus={}, "
                                + "reasonCode={}, errorMessage={}, authMessage={}, bodyPreview={}",
                        requestSummary, apiResponse.status(), reasonCode, errorMessage,
                        authMessage, truncate(rawBody));
                throw new AnalysisExecutionException("관광수요 방문자 API가 요청을 거부했습니다."
                        + " reasonCode=" + reasonCode + ", message="
                        + (authMessage == null ? errorMessage : authMessage));
            }

            JsonNode body = root == null ? null : root.path("response").path("body");
            JsonNode header = root == null ? null : root.path("response").path("header");
            String resultCode = textOrNull(header, "resultCode");
            String resultMsg = textOrNull(header, "resultMsg");
            List<JsonNode> rawItems = body == null ? List.of() : items(body.path("items").path("item"));
            int rawItemCount = rawItems.size();
            List<Raw> parsedAll = rawItems.stream().map(this::raw).filter(Objects::nonNull).toList();
            List<Raw> parsed = parsedAll;
            int parsedItemCount = parsed.size();
            if (rawItemCount > 0 && parsedAll.isEmpty()) {
                log.error("Regional visitor API item parsing produced no rows: {}, firstItemFields={}, firstItem={}",
                        requestSummary, fieldNames(rawItems.get(0)), truncate(rawItems.get(0).toString()));
                throw new AnalysisExecutionException("관광수요 API item 필드를 해석하지 못했습니다. firstItem="
                        + truncate(rawItems.get(0).toString()));
            }
            Raw sample = parsed.isEmpty() ? null : parsed.get(0);
            List<Raw> filteredByType = parsed.stream().filter(row -> isTotal(row) || isComponent(row)).toList();
            List<Raw> filtered = filteredByType.isEmpty() ? parsed : filteredByType;

            log.info("Regional visitor API parsed: {}, httpStatus={}, resultCode={}, resultMsg={}, "
                            + "rootFields={}, bodyFields={}, totalCount={}, rawItemCount={}, parsedItemCount={}, "
                            + "filteredItemCount={}, sampleSignguCode={}, sampleSignguNm={}, sampleTouDivCd={}, "
                            + "sampleTouDivNm={}, sampleTouNum={}",
                    requestSummary, apiResponse.status(), resultCode, resultMsg,
                    root == null ? 0 : root.size(), body == null || body.isMissingNode() ? 0 : body.size(),
                    body == null || body.isMissingNode() ? null : body.path("totalCount").asText(null),
                    rawItemCount, parsedItemCount, filtered.size(),
                    sample == null ? null : sample.code,
                    sample == null ? null : sample.name,
                    sample == null ? null : sample.divCode,
                    sample == null ? null : sample.divName,
                    sample == null ? null : sample.count);
            if (body == null || body.isMissingNode()) {
                log.error("Regional visitor API response body missing: {}, httpStatus={}, contentType={}, "
                                + "bodyLength={}, bodyPreview={}, rootPreview={}",
                        requestSummary, apiResponse.status(), apiResponse.contentType(),
                        lengthOf(rawBody), truncate(rawBody), root == null ? "null" : truncate(root.toString()));
                throw new AnalysisExecutionException("관광수요 방문자 API 응답 본문이 없습니다.");
            }
            total = body.path("totalCount").asInt(0);
            all.addAll(filtered);
            if (rawItemCount < 10_000) break;
            page++;
        }

        if (all.isEmpty()) return List.of();
        return all.stream()
                .collect(Collectors.groupingBy(r -> r.date + "|" + r.code, LinkedHashMap::new, Collectors.toList()))
                .values().stream().map(this::select).filter(Objects::nonNull).toList();
    }

    private VisitorRecord select(List<Raw> rows) {
        Raw total = rows.stream().filter(this::isTotal).findFirst().orElse(null);
        if (total != null) return total.record();
        List<Raw> components = rows.stream().filter(this::isComponent).toList();
        if (components.isEmpty()) return rows.get(0).record();
        Raw first = components.get(0);
        return new VisitorRecord(first.date, first.code, first.name, first.sido,
                components.stream().mapToLong(r -> r.count).sum());
    }

    private boolean isTotal(Raw row) {
        String code = row.divCode.toUpperCase(Locale.ROOT);
        String name = row.divName.toLowerCase(Locale.ROOT);
        return code.equals("ALL") || code.equals("TOTAL") || code.equals("T") || code.equals("0")
                || name.contains("전체") || name.contains("합계") || name.contains("total");
    }

    private boolean isComponent(Raw row) {
        String name = row.divName.toLowerCase(Locale.ROOT);
        return name.contains("내국인") || name.contains("외국인")
                || row.divCode.equalsIgnoreCase("OUTSIDER")
                || row.divCode.equalsIgnoreCase("FOREIGN");
    }

    private Raw raw(JsonNode node) {
        try {
            String date = first(node, "baseYmd");
            String code = first(node, "signguCode", "signguCd");
            String name = first(node, "signguNm", "signguName");
            if (date == null || code == null || name == null) return null;
            return new Raw(parseDate(date), code, name,
                    first(node, "areaNm", "sidoNm"), text(node, "touDivCd"), text(node, "touDivNm"),
                    parseCount(text(node, "touNum")));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value.matches("\\d{8}")) return LocalDate.parse(value, DATE);
        return LocalDate.parse(value);
    }

    private long parseCount(String value) {
        String normalized = value.replace(",", "").trim();
        return new BigDecimal(normalized).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private List<JsonNode> items(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return List.of();
        if (!node.isContainerNode() && node.asText().isBlank()) return List.of();
        if (node.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            node.forEach(result::add);
            return result;
        }
        return List.of(node);
    }

    private String truncate(String value) {
        if (value == null) return "null";
        return value.length() <= 2000 ? value : value.substring(0, 2000) + "...";
    }

    private List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private String requestSummary(String region, LocalDate from, LocalDate to, int page) {
        return "endpoint=" + endpoint + ", region=" + region
                + ", startYmd=" + from.format(DATE) + ", endYmd=" + to.format(DATE)
                + ", pageNo=" + page + ", numOfRows=10000, responseType=json";
    }

    private String redactServiceKey(String uri) {
        return uri.replaceFirst("([?&]serviceKey=)[^&]*", "$1<redacted>");
    }

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("missing " + field);
        return value;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    public record VisitorRecord(LocalDate date, String regionCode, String regionName, String sidoName, long visitorCount) {}
    private record ApiResponse(int status, String contentType, String contentLength, String body) {}
    private record Raw(LocalDate date, String code, String name, String sido, String divCode, String divName, long count) {
        VisitorRecord record() { return new VisitorRecord(date, code, name, sido, count); }
    }
}
