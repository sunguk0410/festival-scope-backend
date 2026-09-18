package likelion.festivalscope.analysis.recommendation.dto;

import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.RecommendationPriority;

public record RecommendationDraft(
        String recommendationType,
        RecommendationPriority priority,
        String title,
        String content,
        Integer displayOrder,
        FestivalAnalysisItem festivalAnalysisItem
) {
}
