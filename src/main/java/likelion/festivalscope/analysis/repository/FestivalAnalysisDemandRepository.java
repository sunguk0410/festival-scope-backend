package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisDemand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalAnalysisDemandRepository extends JpaRepository<FestivalAnalysisDemand, Long> {
    List<FestivalAnalysisDemand> findAllByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId);
}