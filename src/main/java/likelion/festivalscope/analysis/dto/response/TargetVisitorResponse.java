package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.AnalysisItemType;

import java.math.BigDecimal;
import java.util.List;

public record TargetVisitorResponse(
        @Schema(description = "분석 항목 유형", example = "TARGET_VISITOR") AnalysisItemType itemType,
        @Schema(description = "분석 점수", example = "72.50", nullable = true) BigDecimal score,
        TargetVisitor targetVisitor,
        List<RecommendationResponse> recommendations,
        ResultInterpretation resultInterpretation
) {
    public record TargetVisitor(
            @Schema(description = "목표 방문객 수", example = "120000", nullable = true) Long targetVisitorCount,
            @Schema(description = "비교 대상 유사 축제 수", example = "5") Integer similarFestivalCount,
            @Schema(description = "방문객 데이터가 존재하는 비교 대상 수", example = "4") Integer visitorDataCount,
            @Schema(description = "비교 대상 방문객 평균", example = "68500.00", nullable = true) BigDecimal visitorAverage,
            @Schema(description = "비교 대상 방문객 중앙값", example = "74000.00", nullable = true) BigDecimal visitorMedian,
            @Schema(description = "비교 대상 최소 방문객 수", example = "21000", nullable = true) Long visitorMin,
            @Schema(description = "비교 대상 최대 방문객 수", example = "165000", nullable = true) Long visitorMax,
            @Schema(description = "목표 방문객과 비교 기준의 차이율", example = "62.20", nullable = true) BigDecimal gapRate,
            @Schema(description = "유사도 기준값", example = "60.00", nullable = true) BigDecimal similarityThreshold,
            List<SameFestivalHistory> sameFestivalHistories,
            List<SimilarFestival> topSimilarFestivals
    ) {}

    public record SameFestivalHistory(
            @Schema(description = "이력 순위", example = "1") Integer rank,
            Long festivalId, Long festivalHistoryId,
            @Schema(description = "축제명", example = "봄꽃축제") String festivalName,
            @Schema(description = "개최 연도", example = "2025") Integer year,
            BigDecimal budget, Long visitorCount
    ) {}

    public record SimilarFestival(
            Integer rank, Long festivalId, Long festivalHistoryId,
            String festivalName, Integer year, BigDecimal budget, Long visitorCount,
            BigDecimal themeSimilarity, BigDecimal regionSimilarity,
            BigDecimal periodSimilarity, BigDecimal similarityScore
    ) {}
}
