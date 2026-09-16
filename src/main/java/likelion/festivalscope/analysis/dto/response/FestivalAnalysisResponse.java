package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FestivalAnalysisResponse(
        @Schema(description = "FestivalAnalysis ID", example = "1") Long analysisId,
        @Schema(description = "분석 대상 FestivalPlan ID", example = "1") Long festivalPlanId,
        @Schema(description = "축제명", example = "영월 가을별빛 야행축제") String festivalName,
        @Schema(description = "현재 구현된 분석 항목의 종합 점수", example = "70.00", nullable = true) BigDecimal totalScore,
        @Schema(description = "분석 상태", example = "COMPLETED") AnalysisStatus analysisStatus,
        @Schema(description = "분석 생성 시각", example = "2026-09-16T12:30:00") LocalDateTime createdAt,
        @Schema(description = "분석 항목 목록") List<ItemResponse> items
) {
    public record ItemResponse(
            @Schema(description = "FestivalAnalysisItem ID", example = "1") Long itemId,
            @Schema(description = "분석 항목 유형", example = "TARGET_VISITOR") AnalysisItemType itemType,
            @Schema(description = "분석 항목 점수. 아직 계산되지 않은 항목은 null", example = "70.00", nullable = true) BigDecimal score
    ) {}
}