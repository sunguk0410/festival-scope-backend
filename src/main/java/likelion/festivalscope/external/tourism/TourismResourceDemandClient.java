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

@Slf4j
@Component
public class TourismResourceDemandClient {
    private final RestClient client = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String serviceKey;
    private final String baseYm;

    public TourismResourceDemandClient(
            @Value("${tourism.resource-demand.base-url:https://apis.data.go.kr/B551011/AreaTarResDemService}") String baseUrl,
            @Value("${tourism.visitor.service-key:}") String serviceKey,
            @Value("${tourism.linkage.base-ym:202608}") String baseYm) {
        this.baseUrl = baseUrl;
        this.serviceKey = normalizeServiceKey(serviceKey);
        this.baseYm = baseYm;
    }

    public Indicator fetch(String areaCd, String sigunguCd) {
        String url = baseUrl + "/areaCulResDemList?serviceKey=" + serviceKey
                + "&numOfRows=100&pageNo=1&MobileOS=ETC&MobileApp=FestivalScope"
                + "&baseYm=" + baseYm + "&areaCd=" + areaCd + "&signguCd=" + sigunguCd
                + "&culResDemIxCd=12&_type=json";
        try {
            String body = client.get().uri(URI.create(url)).retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("Tourism resource demand API error: status={}, body={}", response.getStatusCode(), errorBody);
                        throw new IllegalStateException("HTTP " + response.getStatusCode());
                    }).body(String.class);
            JsonNode item = item(body);
            if (item == null || !"0000".equals(objectMapper.readTree(body).path("response").path("header").path("resultCode").asText())) return null;
            return new Indicator(text(item, "baseYm"), text(item, "culResDemIxCd"), text(item, "culResDemIxNm"), decimal(item, "culResDemIxVal"));
        } catch (Exception e) {
            log.warn("Tourism resource demand API request failed: areaCd={}, sigunguCd={}, message={}", areaCd, sigunguCd, e.getMessage());
            return null;
        }
    }

    private JsonNode item(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body).path("response").path("body").path("items").path("item");
        return node.isArray() ? (node.isEmpty() ? null : node.get(0)) : (node.isMissingNode() || node.isNull() ? null : node);
    }
    private String text(JsonNode n, String f) { String v = n.path(f).asText(null); return v == null || v.isBlank() ? null : v.trim(); }
    private BigDecimal decimal(JsonNode n, String f) { try { return new BigDecimal(text(n, f)); } catch (Exception e) { return null; } }
    private String normalizeServiceKey(String value) { if (value == null) return ""; String v=value.trim(); return v.length()>=2&&v.startsWith("\"")&&v.endsWith("\"")?v.substring(1,v.length()-1):v; }
    public record Indicator(String baseYm, String code, String name, BigDecimal value) {}
}
