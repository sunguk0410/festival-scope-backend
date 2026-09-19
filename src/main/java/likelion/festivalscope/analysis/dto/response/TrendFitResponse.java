package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.AnalysisItemType;

import java.math.BigDecimal;
import java.util.List;

public record TrendFitResponse(
        @Schema(description = "분석 항목 유형", example = "TREND_FIT") AnalysisItemType itemType,
        @Schema(description = "분석 점수", example = "null", nullable = true) BigDecimal score,
        @Schema(description = "모든 키워드의 연도별 평균 검색 관심도와 증감률") IntegratedTrend integratedTrend,
        @Schema(description = "키워드별 연도별 검색 관심도와 증감률") List<KeywordTrend> keywords,
        @Schema(description = "전년도 개최 시기 기준 전후 3개월 검색 관심도") List<PreviousYearAroundEventPeriod> previousYearAroundEventPeriod,
        List<RecommendationResponse> recommendations,
        ResultInterpretation resultInterpretation
) {
    public record IntegratedTrend(
            @Schema(description = "연도별 통합 검색 관심도", example = "[{\"year\":2025,\"interest\":51.32}]") List<YearlyInterest> yearlyInterest,
            @Schema(description = "전년 대비 통합 검색 관심도 증감률", example = "21.75") List<YearlyGrowthRate> yearlyGrowthRate
    ) {}

    public record KeywordTrend(
            @Schema(description = "검색 키워드", example = "불꽃축제") String keyword,
            @Schema(description = "키워드별 연도별 검색 관심도") List<YearlyInterest> yearlyInterest,
            @Schema(description = "키워드별 전년 대비 검색 관심도 증감률") List<YearlyGrowthRate> yearlyGrowthRate
    ) {}

    public record PreviousYearAroundEventPeriod(
            @Schema(description = "검색 키워드", example = "불꽃축제") String keyword,
            @Schema(description = "전년도 개최 시기 전후 3개월의 월별 검색 관심도") List<MonthlyInterest> monthlyInterest
    ) {}

    public record YearlyInterest(
            @Schema(description = "연도", example = "2025") Integer year,
            @Schema(description = "해당 연도의 평균 검색 관심도 지수", example = "51.32") BigDecimal interest
    ) {}

    public record YearlyGrowthRate(
            @Schema(description = "비교 기준 연도", example = "2024") Integer fromYear,
            @Schema(description = "비교 대상 연도", example = "2025") Integer toYear,
            @Schema(description = "전년 대비 검색 관심도 증감률(%)", example = "21.75", nullable = true) BigDecimal rate
    ) {}

    public record MonthlyInterest(
            @Schema(description = "월", example = "2026-04") String month,
            @Schema(description = "해당 월의 검색 관심도 지수", example = "63.40", nullable = true) BigDecimal interest
    ) {}
}
