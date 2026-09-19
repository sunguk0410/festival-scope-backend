package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.*;

import java.math.BigDecimal;
import java.util.List;

public record TourismLinkageResponse(
        @Schema(description = "분석 항목 유형", example = "TOURISM_LINKAGE") AnalysisItemType itemType,
        @Schema(description = "분석 점수", example = "70.00", nullable = true) BigDecimal score,
        TourismLinkage tourismLinkage,
        List<RecommendationResponse> recommendations,
        ResultInterpretation resultInterpretation
) {
    public record TourismLinkage(
            @Schema(description = "반경 5km 내 전체 후보 POI 수", example = "24") Integer totalCandidatePoiCount,
            @Schema(description = "반경 3km 내 관광·문화 POI 수", example = "12") Integer tourismCultureCount,
            @Schema(description = "반경 3km 내 음식·쇼핑 POI 수", example = "8") Integer foodShoppingCount,
            @Schema(description = "반경 3km 내 숙박 POI 수", example = "4") Integer accommodationCount,
            String tourismLinkageSummary, String consumptionLinkageSummary, String stayLinkageSummary,
            PoiSummary poiSummary, RegionalIndicators regionalIndicators,
            List<Poi> tourismCulture, List<Poi> foodShopping, List<Poi> accommodation,
            PoiRangeGroups tourismCultureByRange, PoiRangeGroups foodShoppingByRange,
            PoiRangeGroups accommodationByRange
    ) {}

    public record PoiRangeGroups(List<Poi> within3km, List<Poi> between3And5km) {}
    public record PoiSummary(RangeCount within3km, RangeCount between3And5km, RangeCount within5km) {}
    public record RangeCount(Integer totalCount, Integer tourismCultureCount, Integer foodShoppingCount, Integer accommodationCount) {}
    public record RegionalIndicators(Indicator resourceDemand, Indicator consumptionIntensity, Indicator stayIntensity) {}
    public record Indicator(String baseYm, String code, String name, BigDecimal value) {}
    public record Poi(String contentId, String poiName, Integer contentTypeId, PoiType poiType,
                      LinkageType linkageType, Integer distanceMeter, PoiDistanceRange distanceRange,
                      BigDecimal latitude, BigDecimal longitude, String address, String imageUrl) {}
}
