package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;

import java.util.List;

public interface RecommendationGenerator {

    AnalysisItemType supports();

    List<RecommendationDraft> generate(RecommendationContext context);
}
