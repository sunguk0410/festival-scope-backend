package likelion.festivalscope.external.tourism;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TourApiClient {
    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String serviceKey;

    public TourApiClient(
            @Value("${tourism.api.base-url:https://apis.data.go.kr/B551011/KorService2}") String baseUrl,
            @Value("${tourism.visitor.service-key:}") String serviceKey) {
        this.client = RestClient.builder().build();
        this.baseUrl = baseUrl;
        this.serviceKey = normalizeServiceKey(serviceKey);
    }

    private String normalizeServiceKey(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    public List<Poi> locationBasedList(BigDecimal latitude, BigDecimal longitude, int radius, int contentTypeId) {
        if (serviceKey.isBlank()) throw new AnalysisExecutionException("TourAPI 인증키가 설정되지 않았습니다.");
        try {
            String requestUrl = baseUrl + "/locationBasedList2"
                    + "?serviceKey=" + serviceKey
                    + "&MobileOS=ETC&MobileApp=FestivalScope&_type=json"
                    + "&mapX=" + longitude
                    + "&mapY=" + latitude
                    + "&radius=" + radius
                    + "&contentTypeId=" + contentTypeId
                    + "&arrange=A&numOfRows=1000";
            String body = client.get().uri(URI.create(requestUrl)).retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("TourAPI error: status={}, body={}", response.getStatusCode(), errorBody);
                        throw new IllegalStateException("TourAPI HTTP " + response.getStatusCode());
                    })
                    .body(String.class);
            return parse(body, contentTypeId);
        } catch (Exception e) {
            if (e instanceof AnalysisExecutionException exception) throw exception;
            throw new AnalysisExecutionException("TourAPI 위치기반 관광정보 조회에 실패했습니다.", e);
        }
    }

    private List<Poi> parse(String body, int requestedType) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode item = root.path("response").path("body").path("items").path("item");
        if (item.isMissingNode() || item.isNull()) return List.of();
        List<JsonNode> nodes = item.isArray() ? toList(item) : List.of(item);
        List<Poi> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            String contentId = text(node, "contentid");
            String title = text(node, "title");
            BigDecimal mapX = decimal(node, "mapx");
            BigDecimal mapY = decimal(node, "mapy");
            if (contentId == null || title == null || mapX == null || mapY == null) continue;
            Integer type = integer(node, "contenttypeid");
            result.add(new Poi(contentId, title, type == null ? requestedType : type, mapX, mapY,
                    text(node, "addr1"), text(node, "firstimage")));
        }
        return result;
    }

    private List<JsonNode> toList(JsonNode node) {
        List<JsonNode> result = new ArrayList<>();
        node.forEach(result::add);
        return result;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        try { return value == null ? null : new BigDecimal(value); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        try { return value == null ? null : Integer.valueOf(value); }
        catch (NumberFormatException e) { return null; }
    }

    public record Poi(String contentId, String title, int contentTypeId, BigDecimal longitude,
                      BigDecimal latitude, String address, String imageUrl) {}
}
