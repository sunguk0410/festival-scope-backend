package likelion.festivalscope.analysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import likelion.festivalscope.analysis.entity.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FestivalAnalysisResponse(
        @Schema(description = "분석 ID", example = "1") Long analysisId,
        @Schema(description = "축제 계획 ID", example = "1") Long festivalPlanId,
        @Schema(description = "축제명", example = "서울 불꽃축제") String festivalName,
        @Schema(description = "전체 분석 점수", example = "72.50", nullable = true) BigDecimal totalScore,
        @Schema(description = "점수 등급", example = "B", nullable = true) String scoreGrade,
        @Schema(description = "분석 상태", example = "COMPLETED") AnalysisStatus analysisStatus,
        @Schema(description = "분석 생성 시각", example = "2026-09-19T12:30:00") LocalDateTime createdAt,
        @Schema(description = "분석 항목별 메인 요약") List<AnalysisItemSummaryResponse> items
) {
}
