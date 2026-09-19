package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FinalReportResponse(
        @Schema(description = "요약 화면 데이터") FestivalAnalysisResponse summary,
        @Schema(description = "해당 분석 전체 추천 목록") List<RecommendationResponse> recommendations
) {
}
