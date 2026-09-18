package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.RecommendationPriority;

public record RecommendationResponse(
        Long recommendationId,
        String recommendationType,
        RecommendationPriority priority,
        String title,
        String content,
        Integer displayOrder
) {
}
