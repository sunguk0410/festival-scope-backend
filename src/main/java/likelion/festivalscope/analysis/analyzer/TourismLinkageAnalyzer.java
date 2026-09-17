package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.analysis.entity.LinkageType;
import likelion.festivalscope.analysis.entity.PoiType;
import likelion.festivalscope.common.util.GeoDistance;
import likelion.festivalscope.external.tourism.TourApiClient;
import likelion.festivalscope.plan.entity.FestivalPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TourismLinkageAnalyzer {
    private static final int TOUR_CULTURE_RADIUS = 3_000;
    private static final int FOOD_SHOPPING_RADIUS = 1_500;
    private static final int ACCOMMODATION_RADIUS = 3_000;
    private final TourApiClient tourApiClient;

    public Result analyze(FestivalPlan plan) {
        if (plan.getLatitude() == null || plan.getLongitude() == null) {
            throw new IllegalArgumentException("FestivalPlan latitude/longitude are required for TOURISM_LINKAGE");
        }
        Map<String, Candidate> unique = new LinkedHashMap<>();
        for (int type : List.of(12, 14, 38, 39, 32)) {
            int radius = type == 38 || type == 39 ? FOOD_SHOPPING_RADIUS :
                    type == 32 ? ACCOMMODATION_RADIUS : TOUR_CULTURE_RADIUS;
            for (TourApiClient.Poi poi : tourApiClient.locationBasedList(plan.getLatitude(), plan.getLongitude(), radius, type)) {
                PoiType poiType = poiType(type);
                int distance = GeoDistance.meters(plan.getLatitude(), plan.getLongitude(), poi.latitude(), poi.longitude());
                if (distance <= radius) unique.putIfAbsent(poi.contentId(), new Candidate(poi, poiType, linkageType(poiType), distance));
            }
        }
        List<Candidate> candidates = unique.values().stream().sorted(Comparator.comparingInt(Candidate::distanceM)).toList();
        int tourismCulture = count(candidates, PoiType.TOURIST_ATTRACTION, PoiType.CULTURAL_FACILITY);
        int foodShopping = count(candidates, PoiType.RESTAURANT, PoiType.SHOPPING);
        int accommodation = count(candidates, PoiType.ACCOMMODATION);
        return new Result(candidates, tourismCulture, foodShopping, accommodation,
                summary("관광·문화", tourismCulture, "주변 관광 활동"),
                summary("음식·쇼핑", foodShopping, "지역 소비 활동"),
                summary("숙박시설", accommodation, "체류형 관광"));
    }

    private PoiType poiType(int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> PoiType.TOURIST_ATTRACTION;
            case 14 -> PoiType.CULTURAL_FACILITY;
            case 38 -> PoiType.SHOPPING;
            case 39 -> PoiType.RESTAURANT;
            case 32 -> PoiType.ACCOMMODATION;
            default -> throw new IllegalArgumentException("Unsupported TourAPI contentTypeId: " + contentTypeId);
        };
    }

    private LinkageType linkageType(PoiType type) {
        return switch (type) {
            case TOURIST_ATTRACTION, CULTURAL_FACILITY -> LinkageType.TOUR_ROUTE;
            case RESTAURANT, SHOPPING -> LinkageType.LOCAL_COMMERCE;
            case ACCOMMODATION -> LinkageType.STAY_EXTENSION;
        };
    }

    private int count(List<Candidate> candidates, PoiType... types) {
        return (int) candidates.stream().filter(c -> Arrays.asList(types).contains(c.poiType())).count();
    }

    private String summary(String category, int count, String activity) {
        if (count == 0) return "설정된 범위 내에서 " + category + "이(가) 확인되지 않아 " + activity + "과의 연계 기반은 제한적입니다.";
        if (count <= 2) return "행사장 인근에 " + category + " " + count + "개가 확인되어 일부 " + activity + "과의 연계가 가능합니다.";
        return "행사장 인근에 " + category + " " + count + "개가 확인되어 " + activity + "으로 연계할 수 있는 기반이 있습니다.";
    }

    public record Candidate(TourApiClient.Poi poi, PoiType poiType, LinkageType linkageType, int distanceM) {}
    public record Result(List<Candidate> candidates, int tourismCultureCount, int foodShoppingCount,
                         int accommodationCount, String tourismLinkageSummary,
                         String consumptionLinkageSummary, String stayLinkageSummary) {
        public int totalCandidatePoiCount() { return candidates.size(); }
    }
}
