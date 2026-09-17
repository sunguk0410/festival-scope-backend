package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.common.util.GeoDistance;
import likelion.festivalscope.common.util.RegionNameNormalizer;
import likelion.festivalscope.external.tourism.TourApiClient;
import likelion.festivalscope.external.tourism.TourismDemandIntensityClient;
import likelion.festivalscope.external.tourism.TourismResourceDemandClient;
import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.region.entity.RegionCode;
import likelion.festivalscope.region.repository.RegionCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourismLinkageAnalyzer {
    private static final int RADIUS_3KM = 3_000;
    private static final int RADIUS_5KM = 5_000;
    private final TourApiClient tourApiClient;
    private final RegionCodeRepository regionCodeRepository;
    private final TourismResourceDemandClient resourceDemandClient;
    private final TourismDemandIntensityClient demandIntensityClient;

    public Result analyze(FestivalPlan plan) {
        if (plan.getLatitude() == null || plan.getLongitude() == null) throw new IllegalArgumentException("FestivalPlan latitude/longitude are required");
        Map<String, Candidate> unique = new LinkedHashMap<>();
        for (int contentTypeId : List.of(12, 14, 38, 39, 32)) {
                for (TourApiClient.Poi poi : tourApiClient.locationBasedList(plan.getLatitude(), plan.getLongitude(), RADIUS_5KM, contentTypeId)) {
                int distance = GeoDistance.meters(plan.getLatitude(), plan.getLongitude(), poi.latitude(), poi.longitude());
                if (poi.contentId() == null || distance > RADIUS_5KM) continue;
                PoiType type = poiType(contentTypeId);
                unique.putIfAbsent(poi.contentId(), new Candidate(poi, type, linkageType(type), distance,
                        distance <= RADIUS_3KM ? PoiDistanceRange.WITHIN_3KM : PoiDistanceRange.BETWEEN_3_AND_5KM));
            }
        }
        List<Candidate> candidates = unique.values().stream().sorted(Comparator.comparingInt(Candidate::distanceM)).toList();
        int tourismCulture = count(candidates, PoiType.TOURIST_ATTRACTION, PoiType.CULTURAL_FACILITY);
        int foodShopping = count(candidates, PoiType.RESTAURANT, PoiType.SHOPPING);
        int accommodation = count(candidates, PoiType.ACCOMMODATION);
        RegionalIndicators indicators = fetchRegionalIndicators(plan);
        return new Result(candidates, tourismCulture, foodShopping, accommodation,
                count(candidates, PoiDistanceRange.WITHIN_3KM, PoiType.TOURIST_ATTRACTION, PoiType.CULTURAL_FACILITY),
                count(candidates, PoiDistanceRange.BETWEEN_3_AND_5KM, PoiType.TOURIST_ATTRACTION, PoiType.CULTURAL_FACILITY),
                count(candidates, PoiDistanceRange.WITHIN_3KM, PoiType.RESTAURANT, PoiType.SHOPPING),
                count(candidates, PoiDistanceRange.BETWEEN_3_AND_5KM, PoiType.RESTAURANT, PoiType.SHOPPING),
                count(candidates, PoiDistanceRange.WITHIN_3KM, PoiType.ACCOMMODATION),
                count(candidates, PoiDistanceRange.BETWEEN_3_AND_5KM, PoiType.ACCOMMODATION), indicators,
                summary("관광·문화자원", tourismCulture, "주변 관광 활동", indicators.resource()),
                summary("음식·쇼핑 자원", foodShopping, "지역 소비 활동", indicators.consumption()),
                summary("숙박시설", accommodation, "체류형 관광", indicators.stay()));
    }

    private RegionalIndicators fetchRegionalIndicators(FestivalPlan plan) {
        RegionCode code = regionCodeRepository.findAll().stream().filter(row ->
                RegionNameNormalizer.sameSido(plan.getSido(), row.getSidoName()) && RegionNameNormalizer.sameSigungu(plan.getSigungu(), row.getSigunguName())).findFirst().orElse(null);
        if (code == null) {
            log.warn("Tourism region code mapping failed: sido={}, sigungu={}", plan.getSido(), plan.getSigungu());
            return new RegionalIndicators(null, null, null);
        }
        return new RegionalIndicators(resourceDemandClient.fetch(code.getAreaCd(), code.getSigunguCd()),
                demandIntensityClient.fetchConsumption(code.getAreaCd(), code.getSigunguCd()),
                demandIntensityClient.fetchStay(code.getAreaCd(), code.getSigunguCd()));
    }

    private String summary(String category, int count, String activity, Object indicator) {
        if (indicator != null) {
            String value = indicator instanceof TourismResourceDemandClient.Indicator r ? String.valueOf(r.value()) : String.valueOf(((TourismDemandIntensityClient.Indicator) indicator).value());
            if (count == 0) return "해당 지역의 관광 지표는 " + value + "로 확인되지만, 행사장 5km 내 " + category + "은 제한적으로 확인됩니다.";
            return "행사장 주변에 " + category + "이 분포하고 있으며, 해당 지역의 관광 지표는 " + value + "로 확인됩니다.";
        }
        if (count == 0) return "설정된 범위 내에서 " + category + "이 확인되지 않아 " + activity + " 연계 기반은 제한적입니다.";
        return "행사장 주변에 " + category + "이 확인되어 " + activity + "으로 연계할 수 있는 기본적인 자원 기반이 있습니다.";
    }

    private PoiType poiType(int id) { return switch (id) { case 12 -> PoiType.TOURIST_ATTRACTION; case 14 -> PoiType.CULTURAL_FACILITY; case 38 -> PoiType.SHOPPING; case 39 -> PoiType.RESTAURANT; case 32 -> PoiType.ACCOMMODATION; default -> throw new IllegalArgumentException("Unsupported contentTypeId: " + id); }; }
    private LinkageType linkageType(PoiType type) { return switch (type) { case TOURIST_ATTRACTION, CULTURAL_FACILITY -> LinkageType.TOUR_ROUTE; case RESTAURANT, SHOPPING -> LinkageType.LOCAL_COMMERCE; case ACCOMMODATION -> LinkageType.STAY_EXTENSION; }; }
    private int count(List<Candidate> rows, PoiType... types) { return (int) rows.stream().filter(row -> Arrays.asList(types).contains(row.poiType())).count(); }
    private int count(List<Candidate> rows, PoiDistanceRange range, PoiType... types) { return (int) rows.stream().filter(row -> row.distanceRange() == range && Arrays.asList(types).contains(row.poiType())).count(); }

    public record Candidate(TourApiClient.Poi poi, PoiType poiType, LinkageType linkageType, int distanceM, PoiDistanceRange distanceRange) {}
    public record RegionalIndicators(TourismResourceDemandClient.Indicator resource, TourismDemandIntensityClient.Indicator consumption, TourismDemandIntensityClient.Indicator stay) {}
    public record Result(List<Candidate> candidates, int tourismCultureCount, int foodShoppingCount, int accommodationCount,
                         int tourismCultureWithin3kmCount, int tourismCultureBetween3And5kmCount, int foodShoppingWithin3kmCount, int foodShoppingBetween3And5kmCount,
                         int accommodationWithin3kmCount, int accommodationBetween3And5kmCount, RegionalIndicators indicators,
                         String tourismLinkageSummary, String consumptionLinkageSummary, String stayLinkageSummary) {
        public int totalCandidatePoiCount() { return candidates.size(); }
        public int tourismCultureWithin5kmCount() { return tourismCultureWithin3kmCount + tourismCultureBetween3And5kmCount; }
        public int foodShoppingWithin5kmCount() { return foodShoppingWithin3kmCount + foodShoppingBetween3And5kmCount; }
        public int accommodationWithin5kmCount() { return accommodationWithin3kmCount + accommodationBetween3And5kmCount; }
    }
}
