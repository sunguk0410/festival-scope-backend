package likelion.festivalscope.external.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

@Component
@Slf4j
public class BusStopClient {
    private final RestClient client = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String url;
    private final String key;

    public BusStopClient(
            @Value("${bus.stop-url:https://apis.data.go.kr/1613000/BusStop/getBusStop}") String url,
            @Value("${bus.service-key:}") String key) {
        this.url = url;
        this.key = key;
    }

    public List<BusStop> findNearby(BigDecimal latitude, BigDecimal longitude, int radius) {
        if (url.isBlank() || key.isBlank()) {
            throw new AnalysisExecutionException("버스정류장 API 설정이 없습니다.");
        }
        try {
            URI requestUri = URI.create(url
                    + "?serviceKey=" + key
                    + "&gpsLati=" + latitude
                    + "&gpsLong=" + longitude
                    + "&numOfRows=100&pageNo=1&_type=json&radius=" + radius);
            log.info("Nearby bus stop API request: uri={}", redactServiceKey(requestUri.toString()));
            String rawBody = client.get().uri(requestUri).retrieve().body(String.class);
            JsonNode root = rawBody == null || rawBody.isBlank() ? null : objectMapper.readTree(rawBody);
            JsonNode item = root == null ? null : root.at("/response/body/items/item");
            if (item == null || item.isMissingNode()) item = root == null ? null : root.path("items").path("item");
            List<JsonNode> items = normalize(item);
            List<BusStop> stops = items.stream().map(this::parse).filter(Objects::nonNull).toList();
            log.info("Nearby bus stop API response: itemCount={}, parsedCount={}", items.size(), stops.size());
            return stops;
        } catch (Exception exception) {
            log.error("Nearby bus stop API request failed: latitude={}, longitude={}, radius={}, "
                            + "url={}, exceptionType={}, message={}",
                    latitude, longitude, radius, url, exception.getClass().getName(), exception.getMessage(), exception);
            throw new AnalysisExecutionException("버스정류장 API 호출에 실패했습니다.", exception);
        }
    }

    private String redactServiceKey(String uri) {
        return uri.replaceFirst("([?&]serviceKey=)[^&]*", "$1<redacted>");
    }

    private BusStop parse(JsonNode node) {
        try {
            String id = first(node, "nodeid", "stopId", "nodeId", "arsId", "id");
            String name = first(node, "nodenm", "stopName", "nodeName", "name");
            String cityCode = first(node, "citycode", "cityCode");
            String ctpvCode = first(node, "ctpv_cd", "ctpvCode");
            String sggCode = first(node, "sgg_cd", "sggCode");
            BigDecimal latitude = decimal(node, "gpslati", "latitude", "lat", "y");
            BigDecimal longitude = decimal(node, "gpslong", "longitude", "lng", "lon", "x");
            if (id == null || name == null || latitude == null || longitude == null) return null;
            return new BusStop(id, name, cityCode, ctpvCode, sggCode, latitude, longitude);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<JsonNode> normalize(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return List.of();
        if (!node.isContainerNode() && node.asText().isBlank()) return List.of();
        if (node.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            node.forEach(result::add);
            return result;
        }
        return List.of(node);
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        String value = first(node, fields);
        return value == null ? null : new BigDecimal(value);
    }

    public record BusStop(String id, String name, String cityCode, String ctpvCode, String sggCode, BigDecimal latitude, BigDecimal longitude) {}
}
