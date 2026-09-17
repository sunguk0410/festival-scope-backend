package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.LinkageType;
import likelion.festivalscope.analysis.entity.PoiType;
import java.math.BigDecimal;
import java.util.List;

public record TourismLinkageResponse(AnalysisItemType itemType, BigDecimal score, TourismLinkage tourismLinkage) {
    public record TourismLinkage(Integer totalCandidatePoiCount, Integer tourismCultureCount,
                                 Integer foodShoppingCount, Integer accommodationCount,
                                 String tourismLinkageSummary, String consumptionLinkageSummary,
                                 String stayLinkageSummary, List<Poi> tourismCulture,
                                 List<Poi> foodShopping, List<Poi> accommodation) {}
    public record Poi(String contentId, String poiName, Integer contentTypeId, PoiType poiType,
                      LinkageType linkageType, Integer distanceMeter, BigDecimal latitude,
                      BigDecimal longitude, String address, String imageUrl) {}
}
