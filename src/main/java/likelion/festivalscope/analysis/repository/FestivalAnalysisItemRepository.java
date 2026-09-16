package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FestivalAnalysisItemRepository extends JpaRepository<FestivalAnalysisItem, Long> {
    Optional<FestivalAnalysisItem> findByFestivalAnalysis_FestivalAnalysisIdAndItemType(Long analysisId, AnalysisItemType itemType);
    List<FestivalAnalysisItem> findAllByFestivalAnalysis_FestivalAnalysisIdOrderByFestivalAnalysisItemIdAsc(Long analysisId);
}
