package likelion.festivalscope.external.bus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class BusRouteClient {
    private static final String DEFAULT_URL = "https://apis.data.go.kr/1613000/NumberofRoutesServingEachStop/getNumberofRoutesServingEachStop";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient client = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String url;
    private final String key;
    private final String operationDate;
    private final String defaultCtpvCode;
    private final String defaultSggCode;

    public BusRouteClient(
            @Value("${bus.route-url:" + DEFAULT_URL + "}") String url,
            @Value("${bus.route.service-key:}") String key,
            @Value("${bus.route.operation-date:}") String operationDate,
            @Value("${bus.route.ctpv-code:}") String defaultCtpvCode,
            @Value("${bus.route.sgg-code:}") String defaultSggCode) {
        this.url = url;
        this.key = key;
        this.operationDate = operationDate;
        this.defaultCtpvCode = defaultCtpvCode;
        this.defaultSggCode = defaultSggCode;
    }

    public int findRouteCount(String stopId, String ctpvCode, String sggCode) {
        String ctpv = valueOrDefault(ctpvCode, defaultCtpvCode);
        String sgg = valueOrDefault(sggCode, defaultSggCode);
        if (url.isBlank() || key.isBlank() || ctpv.isBlank() || sgg.isBlank()) {
            throw new AnalysisExecutionException("버스 정차 노선수 API 설정이 부족합니다.");
        }

        String date = operationDate.isBlank()
                ? LocalDate.now().format(DATE_FORMAT)
                : operationDate;
        try {
            URI requestUri = URI.create(url
                    + "?serviceKey=" + key
                    + "&pageNo=1&numOfRows=1"
                    + "&opr_ymd=" + date
                    + "&ctpv_cd=" + ctpv
                    + "&sgg_cd=" + sgg
                    + "&sttn_id=" + stopId
                    + "&dataType=JSON");

            String rawBody = client.get().uri(requestUri).retrieve().body(String.class);
            JsonNode root = rawBody == null || rawBody.isBlank() ? null : objectMapper.readTree(rawBody);
            validateResponse(root);
            JsonNode item = root == null ? null : root.at("/response/body/items/item");
            if (item == null || item.isMissingNode()) {
                item = root == null ? null : root.path("items").path("item");
            }
            if (item == null || item.isMissingNode() || item.isNull()) {
                return 0;
            }
            if (item.isArray()) {
                return item.isEmpty() ? 0 : item.get(0).path("rte_cnt").asInt(0);
            }
            return item.path("rte_cnt").asInt(0);
        } catch (AnalysisExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisExecutionException("버스 정차 노선수 API 호출에 실패했습니다.", exception);
        }
    }

    private void validateResponse(JsonNode root) {
        JsonNode header = root == null ? null : root.at("/response/header");
        String resultCode = header == null ? null : header.path("resultCode").asText(null);
        if (resultCode != null && !resultCode.isBlank()
                && !resultCode.equals("00") && !resultCode.equals("0000")) {
            String resultMessage = header.path("resultMsg").asText("");
            throw new AnalysisExecutionException("버스 정차 노선수 API 오류: " + resultCode + " " + resultMessage);
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
