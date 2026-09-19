package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.AnalysisItemType;

import java.math.BigDecimal;
import java.util.List;

public record DemandFitResponse(
        @Schema(description = "분석 항목 유형", example = "DEMAND_FIT") AnalysisItemType itemType,
        @Schema(description = "분석 점수", example = "78.00", nullable = true) BigDecimal score,
        RegionalDemand regionalDemand,
        SeasonalDemand seasonalDemand,
        Accessibility accessibility,
        List<RecommendationResponse> recommendations,
        ResultInterpretation resultInterpretation
) {
    public record RegionalDemand(
            String sido, String region, Long visitorCount, BigDecimal comparisonAverage,
            BigDecimal comparisonMedian,
            @Schema(description = "지역 관광수요 순위", example = "4") Integer rank,
            @Schema(description = "비교 지역 수", example = "17") Integer totalRegions,
            @Schema(description = "지역 관광수요 백분위", example = "82.40") BigDecimal percentile,
            List<ComparisonRegion> comparisonRegions
    ) {}

    public record ComparisonRegion(String region, Long visitorCount, Integer rank) {}

    public record SeasonalDemand(
            @Schema(description = "개최월", example = "8") Integer eventMonth,
            BigDecimal eventMonthAverage,
            List<MonthlyDemand> monthlyAverage,
            @Schema(description = "개최월 관광수요 순위", example = "2") Integer eventMonthRank,
            @Schema(description = "개최월 관광수요 백분위", example = "91.00") BigDecimal eventMonthPercentile,
            List<WeeklyDemand> weeklyDemand,
            Integer recommendedWeek
    ) {}

    public record MonthlyDemand(
            @Schema(description = "월", example = "8") Integer month,
            @Schema(description = "월별 평균 방문객 수", example = "74000.00") BigDecimal visitorAverage
    ) {}

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
