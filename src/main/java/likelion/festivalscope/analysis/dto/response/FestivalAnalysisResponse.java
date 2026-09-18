package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FestivalAnalysisResponse(
        Long analysisId,
        Long festivalPlanId,
        String festivalName,
        BigDecimal totalScore,
        AnalysisStatus analysisStatus,
        LocalDateTime createdAt,
        List<AnalysisItemSummaryResponse> items
) {
}
