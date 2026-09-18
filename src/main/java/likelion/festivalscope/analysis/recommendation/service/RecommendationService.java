package likelion.festivalscope.analysis.recommendation.service;

import likelion.festivalscope.analysis.entity.FestivalAnalysis;
import likelion.festivalscope.analysis.entity.FestivalAnalysisRecommendation;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import likelion.festivalscope.analysis.recommendation.generator.RecommendationGenerator;
import likelion.festivalscope.analysis.recommendation.repository.FestivalAnalysisRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final FestivalAnalysisRecommendationRepository recommendationRepository;
    private final RecommendationContextFactory contextFactory;
    private final List<RecommendationGenerator> generators;

    @Transactional
    public void replaceForAnalysis(FestivalAnalysis analysis) {
        recommendationRepository.deleteAllByFestivalAnalysis_FestivalAnalysisId(
                analysis.getFestivalAnalysisId());

        RecommendationContext context = contextFactory.create(analysis);
        List<FestivalAnalysisRecommendation> recommendations = generators.stream()
                .flatMap(generator -> generator.generate(context).stream())
                .sorted(Comparator.comparing(RecommendationDraft::displayOrder))
                .map(draft -> toEntity(analysis, draft))
                .toList();
        recommendationRepository.saveAll(recommendations);
    }

    private FestivalAnalysisRecommendation toEntity(
            FestivalAnalysis analysis, RecommendationDraft draft) {
        return FestivalAnalysisRecommendation.builder()
                .festivalAnalysis(analysis)
                .festivalAnalysisItem(draft.festivalAnalysisItem())
                .recommendationType(draft.recommendationType())
                .priority(draft.priority())
                .title(draft.title())
                .content(draft.content())
                .displayOrder(draft.displayOrder())
                .build();
    }
}
