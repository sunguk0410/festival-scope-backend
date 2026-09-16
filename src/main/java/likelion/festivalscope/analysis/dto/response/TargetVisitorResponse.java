package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.AnalysisItemType;

import java.math.BigDecimal;
import java.util.List;

public record TargetVisitorResponse(
        @Schema(description = "분석 항목 유형", example = "TARGET_VISITOR") AnalysisItemType itemType,
        @Schema(description = "목표 방문객 타당성 점수", example = "70.00") BigDecimal score,
        @Schema(description = "기획안의 목표 방문객 수", example = "80000") Long targetVisitorCount,
        @Schema(description = "유사 축제 Top 5의 방문객 수 중앙값", example = "51000") BigDecimal similarVisitorMedian,
        @Schema(description = "목표 방문객 수를 유사 축제 중앙값으로 나눈 비율", example = "1.57") BigDecimal targetMedianRatio,
        @Schema(description = "유사 축제 Top 5 목록") List<SimilarFestivalResponse> similarFestivals
) {
    public record SimilarFestivalResponse(
            @Schema(description = "유사 축제명", example = "영월 단종문화제") String festivalName,
            @Schema(description = "참조 연도", example = "2025") Integer year,
            @Schema(description = "해당 연도의 방문객 수", example = "55000") Long visitorCount,
            @Schema(description = "유사도 점수", example = "91.20") BigDecimal similarityScore,
            @Schema(description = "유사 축제 순위", example = "1") Integer rank
    ) {}
}