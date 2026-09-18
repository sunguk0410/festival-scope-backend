package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.*;
import java.math.BigDecimal;
import java.util.List;

public record TourismLinkageResponse(AnalysisItemType itemType, BigDecimal score, TourismLinkage tourismLinkage,
                                     List<RecommendationResponse> recommendations,
                                     ResultInterpretation resultInterpretation) {
    public record TourismLinkage(Integer totalCandidatePoiCount, Integer tourismCultureCount,
                                 Integer foodShoppingCount, Integer accommodationCount,
                                 String tourismLinkageSummary, String consumptionLinkageSummary,
                                 String stayLinkageSummary, PoiSummary poiSummary,
                                 RegionalIndicators regionalIndicators, List<Poi> tourismCulture,
                                 List<Poi> foodShopping, List<Poi> accommodation,
                                 PoiRangeGroups tourismCultureByRange, PoiRangeGroups foodShoppingByRange,
                                 PoiRangeGroups accommodationByRange) {}
    public record PoiRangeGroups(List<Poi> within3km, List<Poi> between3And5km) {}
    public record PoiSummary(RangeCount within3km, RangeCount between3And5km, RangeCount within5km) {}
    public record RangeCount(Integer totalCount, Integer tourismCultureCount, Integer foodShoppingCount, Integer accommodationCount) {}
    public record RegionalIndicators(Indicator resourceDemand, Indicator consumptionIntensity, Indicator stayIntensity) {}
    public record Indicator(String baseYm, String code, String name, BigDecimal value) {}
    public record Poi(String contentId, String poiName, Integer contentTypeId, PoiType poiType,
                      LinkageType linkageType, Integer distanceMeter, PoiDistanceRange distanceRange,
                      BigDecimal latitude, BigDecimal longitude, String address, String imageUrl) {}
}
