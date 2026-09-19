package likelion.festivalscope.analysis.recommendation.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalAnalysisRecommendationRepository
        extends JpaRepository<FestivalAnalysisRecommendation, Long> {

    void deleteAllByFestivalAnalysis_FestivalAnalysisId(Long analysisId);

    List<FestivalAnalysisRecommendation>
    findAllByFestivalAnalysis_FestivalAnalysisIdOrderByDisplayOrderAscRecommendationIdAsc(Long analysisId);

    List<FestivalAnalysisRecommendation>
    findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByDisplayOrderAscRecommendationIdAsc(Long itemId);
}
