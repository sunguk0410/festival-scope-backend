package likelion.festivalscope.analysis.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalysisListResponse(
        Long analysisId,
        String festivalName,
        String hostRegion,
        LocalDateTime inputDate,
        BigDecimal overallScore,
        LocalDate festivalStartDate,
        LocalDate festivalEndDate,
        Long recommendationCount
) {
    public static AnalysisListResponse from(AnalysisListProjection projection) {
        String sido = projection.getSido();
        String sigungu = projection.getSigungu();
        String hostRegion = sigungu == null || sigungu.isBlank()
                ? sido
                : sido + " " + sigungu;
        return new AnalysisListResponse(
                projection.getAnalysisId(),
                projection.getFestivalName(),
                hostRegion,
                projection.getInputDate(),
                projection.getOverallScore(),
                projection.getFestivalStartDate(),
                projection.getFestivalEndDate(),
                projection.getRecommendationCount());
    }

    public interface AnalysisListProjection {
        Long getAnalysisId();
        String getFestivalName();
        String getSido();
        String getSigungu();
        LocalDateTime getInputDate();
        BigDecimal getOverallScore();
        LocalDate getFestivalStartDate();
        LocalDate getFestivalEndDate();
        Long getRecommendationCount();
    }
}
