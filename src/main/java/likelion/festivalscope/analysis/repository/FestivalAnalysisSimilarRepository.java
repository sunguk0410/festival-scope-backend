package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisSimilar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalAnalysisSimilarRepository extends JpaRepository<FestivalAnalysisSimilar, Long> {
    List<FestivalAnalysisSimilar> findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByRankOrderAsc(Long itemId);
}
