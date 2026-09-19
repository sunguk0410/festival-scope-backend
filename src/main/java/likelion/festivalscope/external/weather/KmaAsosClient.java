package likelion.festivalscope.external.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.external.weather.dto.AsosDailyWeatherDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KmaAsosClient {
    private final RestClient client;
    private final DefaultUriBuilderFactory uriBuilderFactory;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public KmaAsosClient(@Value("${kma.asos.api-url:https://apis.data.go.kr/1360000/AsosDalyInfoService/getWthrDataList}") String apiUrl,
                         @Value("${kma.asos.api-key:}") String serviceKey) {
        this.uriBuilderFactory = new DefaultUriBuilderFactory(apiUrl);
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        this.client = RestClient.builder().uriBuilderFactory(uriBuilderFactory).build();
        this.objectMapper = new ObjectMapper();
        this.serviceKey = serviceKey;
    }

    public List<AsosDailyWeatherDto> getDailyWeather(String stationId, LocalDate from, LocalDate to) {
        if (serviceKey == null || serviceKey.isBlank() || serviceKey.contains("${"))
            throw new AnalysisExecutionException("KMA_ASOS_API_KEY가 설정되지 않았습니다.");
        try {
            log.info("KMA ASOS daily request: stationId={}, from={}, to={}", stationId, from, to);
            List<AsosDailyWeatherDto> result = new ArrayList<>();
            int pageNo = 1;
            int totalCount;
            do {
                int requestedPage = pageNo;
                URI requestUri = clientUri(uri -> uri.queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", requestedPage).queryParam("numOfRows", 100)
                        .queryParam("dataType", "JSON").queryParam("dataCd", "ASOS")
                        .queryParam("dateCd", "DAY").queryParam("stnIds", stationId)
                        .queryParam("startDt", from.toString().replace("-", ""))
                        .queryParam("endDt", to.toString().replace("-", "")).build());
                log.info("KMA ASOS request parameters: {}", requestUri.getQuery().replaceFirst("serviceKey=[^&]*", "serviceKey=***"));
                String body = client.get().uri(requestUri).retrieve().body(String.class);
                JsonNode root = objectMapper.readTree(body);
                JsonNode header = root.path("response").path("header");
                String resultCode = header.path("resultCode").asText();
                String resultMsg = header.path("resultMsg").asText();
                JsonNode responseBody = root.path("response").path("body");
                JsonNode items = responseBody.path("items").path("item");
                totalCount = responseBody.path("totalCount").asInt(0);
                if (items.isMissingNode() || items.isNull()) {
                    if (requestedPage == 1) {
                        log.warn("KMA ASOS returned no items: stationId={}, from={}, to={}, resultCode={}, resultMsg={}, body={}",
                                stationId, from, to, resultCode, resultMsg, abbreviate(body));
                        if (!resultCode.isBlank() && !"00".equals(resultCode))
                            throw new IllegalStateException("KMA ASOS resultCode=" + resultCode + ", resultMsg=" + resultMsg);
                    }
                    break;
                }
                Iterable<JsonNode> itemNodes = items.isArray() ? items : List.of(items);
                int pageItemCount = 0;
                for (JsonNode item : itemNodes) {
                    result.add(new AsosDailyWeatherDto(LocalDate.parse(text(item, "tm")),
                            decimal(item, "sumRn"), decimal(item, "avgTa"), decimal(item, "maxTa"), decimal(item, "minTa"),
                            decimal(item, "avgWs"), decimal(item, "maxWs")));
                    pageItemCount++;
                }
                if (pageItemCount == 0) break;
                pageNo++;
            } while ((totalCount > 0 && result.size() < totalCount) || (totalCount == 0 && pageNo == 2));
            log.info("KMA ASOS daily response: stationId={}, from={}, to={}, observationCount={}",
                    stationId, from, to, result.size());
            return result;
        } catch (RestClientResponseException e) {
            log.error("KMA ASOS request failed: stationId={}, from={}, to={}, status={}, body={}",
                    stationId, from, to, e.getStatusCode().value(), abbreviate(e.getResponseBodyAsString()));
            throw new AnalysisExecutionException("기상청 ASOS API 호출에 실패했습니다. status="
                    + e.getStatusCode().value() + ", message=" + abbreviate(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            log.error("KMA ASOS response parsing failed: stationId={}, from={}, to={}", stationId, from, to, e);
            throw new AnalysisExecutionException("기상청 ASOS 일자료 조회 또는 응답 파싱에 실패했습니다.", e);
        }
    }

    private String text(JsonNode node, String field) { return node.path(field).asText(); }
    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        return value.isBlank() || "-".equals(value) ? null : new BigDecimal(value);
    }

    private String abbreviate(String value) {
        if (value == null) return "";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300);
    }

    private URI clientUri(java.util.function.Function<org.springframework.web.util.UriBuilder, URI> builder) {
        return builder.apply(uriBuilderFactory.builder());
    }

}
