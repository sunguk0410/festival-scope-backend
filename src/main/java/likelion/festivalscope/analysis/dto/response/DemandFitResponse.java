package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import java.math.BigDecimal;
import java.util.List;

public record DemandFitResponse(AnalysisItemType itemType, BigDecimal score,
                                RegionalDemand regionalDemand, SeasonalDemand seasonalDemand,
                                Accessibility accessibility,
                                List<RecommendationResponse> recommendations) {
    public record RegionalDemand(String sido, String region, Long visitorCount, BigDecimal comparisonAverage,
                                 BigDecimal comparisonMedian, Integer rank, Integer totalRegions,
                                 BigDecimal percentile, List<ComparisonRegion> comparisonRegions) {}
    public record ComparisonRegion(String region, Long visitorCount, Integer rank) {}
    public record SeasonalDemand(Integer eventMonth, BigDecimal eventMonthAverage,
                                 List<MonthlyDemand> monthlyAverage, Integer eventMonthRank,
                                 BigDecimal eventMonthPercentile, List<WeeklyDemand> weeklyDemand,
                                 Integer recommendedWeek) {}
    public record MonthlyDemand(Integer month, BigDecimal visitorAverage) {}
    public record WeeklyDemand(Integer week, Integer startDay, Integer endDay, Integer days,
                               BigDecimal averageDailyVisitors, Integer rank, Boolean referenceOnly) {}
    public record Accessibility(Bus bus, Rail rail, Parking parking) {}
    public record Bus(String nearestStopName, Integer nearestStopDistanceM, Integer stopCount500m,
                      Integer stopCount1km, Integer routeCount) {}
    public record Rail(Boolean available, String nearestStationName, Integer nearestStationDistanceM,
                       String nearestStationLines, Integer stationsWithin1Km,
                       List<NearbyStation> nearbyStations) {}
    public record NearbyStation(String stationName, Integer distanceMeters, String lineName) {}
    public record Parking(Integer parkingCount, Integer parkingCapacity) {}
}
