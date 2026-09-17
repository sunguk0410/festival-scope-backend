package likelion.festivalscope.external.tourism;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class TourismDemandIntensityClient {
    private final RestClient client = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String serviceKey;
    private final String baseYm;

    public TourismDemandIntensityClient(
            @Value("${tourism.demand-intensity.base-url:https://apis.data.go.kr/B551011/AreaTarDemDsService}") String baseUrl,
            @Value("${tourism.visitor.service-key:}") String serviceKey,
            @Value("${tourism.linkage.base-ym:202608}") String baseYm) {
        this.baseUrl = baseUrl; this.serviceKey = normalizeServiceKey(serviceKey); this.baseYm = baseYm;
    }

    public Indicator fetchStay(String areaCd, String sigunguCd) { return fetch("/areaTarSjrnDsList", "tarSjrnDsIxCd", "21", "tarSjrnDsIxVal", "tarSjrnDsIxNm", areaCd, sigunguCd); }
    public Indicator fetchConsumption(String areaCd, String sigunguCd) { return fetch("/areaTarExpDsList", "tarExpDsIxCd", "22", "tarExpDsIxVal", "tarExpDsIxNm", areaCd, sigunguCd); }

    private Indicator fetch(String endpoint, String codeParam, String code, String valueField, String nameField, String areaCd, String sigunguCd) {
        String url = baseUrl + endpoint + "?serviceKey=" + serviceKey
                + "&numOfRows=100&pageNo=1&MobileOS=ETC&MobileApp=FestivalScope"
                + "&baseYm=" + baseYm + "&areaCd=" + areaCd + "&signguCd=" + sigunguCd
                + "&" + codeParam + "=" + code + "&_type=json";
        try {
            String body = client.get().uri(URI.create(url)).retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("Tourism demand intensity API error: endpoint={}, status={}, body={}", endpoint, response.getStatusCode(), errorBody);
                        throw new IllegalStateException("HTTP " + response.getStatusCode());
                    }).body(String.class);
            JsonNode root = objectMapper.readTree(body);
            if (!"0000".equals(root.path("response").path("header").path("resultCode").asText())) return null;
            JsonNode node = root.path("response").path("body").path("items").path("item");
            if (node.isArray()) node = node.isEmpty() ? null : node.get(0);
            if (node == null || node.isMissingNode() || node.isNull()) return null;
            return new Indicator(text(node, "baseYm"), text(node, codeParam), text(node, nameField), decimal(node, valueField));
        } catch (Exception e) {
            log.warn("Tourism demand intensity API request failed: endpoint={}, areaCd={}, sigunguCd={}, message={}", endpoint, areaCd, sigunguCd, e.getMessage());
            return null;
        }
    }
    private String text(JsonNode n, String f) { String v=n.path(f).asText(null); return v==null||v.isBlank()?null:v.trim(); }
    private BigDecimal decimal(JsonNode n, String f) { try { return new BigDecimal(text(n,f)); } catch (Exception e) { return null; } }
    private String normalizeServiceKey(String value) { if(value==null)return ""; String v=value.trim(); return v.length()>=2&&v.startsWith("\"")&&v.endsWith("\"")?v.substring(1,v.length()-1):v; }
    public record Indicator(String baseYm, String code, String name, BigDecimal value) {}
}
