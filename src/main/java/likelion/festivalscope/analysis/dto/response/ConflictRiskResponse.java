package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConflictRiskResponse(AnalysisItemType itemType, BigDecimal score, ConflictRisk conflictRisk,
                                   List<RecommendationResponse> recommendations) {
    public record ConflictRisk(TargetPeriod targetPeriod, HistoryPeriod historyPeriod,
                               Integer directOverlapCount, Integer nearbyPeriodCount,
                               Integer historicalSamePeriodCount, Integer sameRegionCount,
                               List<Event> events) {}
    public record TargetPeriod(LocalDate startDate, LocalDate endDate) {}
    public record HistoryPeriod(Integer startYear, Integer endYear) {}
    public record Event(Long festivalId, String eventName, Integer eventYear, String sido, String sigungu,
                        RegionRelation regionRelation, LocalDate startDate, LocalDate endDate,
                        EventBasis eventBasis, ConflictType conflictType, Integer overlapDays,
                        Boolean sameTheme, Long visitorCount) {}
}
