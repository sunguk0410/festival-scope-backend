package likelion.festivalscope.analysis.service;

import likelion.festivalscope.analysis.dto.response.RecommendationResponse;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.FestivalAnalysisRecommendation;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.recommendation.repository.FestivalAnalysisRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationQueryService {
    private final FestivalAnalysisRecommendationRepository repository;

    public List<RecommendationResponse> findAll(Long analysisId) {
        return repository.findAllByFestivalAnalysis_FestivalAnalysisIdOrderByDisplayOrderAscRecommendationIdAsc(analysisId)
                .stream().map(this::toResponse).sorted(comparator()).toList();
    }

    public List<RecommendationResponse> findForItem(FestivalAnalysisItem item) {
        return repository.findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByDisplayOrderAscRecommendationIdAsc(item.getFestivalAnalysisItemId())
                .stream().map(this::toResponse).sorted(comparator()).toList();
    }

    private RecommendationResponse toResponse(FestivalAnalysisRecommendation recommendation) {
        return new RecommendationResponse(recommendation.getRecommendationId(), recommendation.getRecommendationType(),
                recommendation.getPriority(), recommendation.getTitle(), recommendation.getContent(), recommendation.getDisplayOrder());
    }

    private Comparator<RecommendationResponse> comparator() {
        return Comparator.comparingInt((RecommendationResponse value) -> priorityOrder(value.priority()))
                .thenComparing(RecommendationResponse::displayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(RecommendationResponse::recommendationId, Comparator.nullsLast(Long::compareTo));
    }

    private int priorityOrder(RecommendationPriority priority) {
        return switch (priority) {
            case IMMEDIATE -> 0;
            case REVIEW -> 1;
            case OPTIONAL -> 2;
        };
    }
}
