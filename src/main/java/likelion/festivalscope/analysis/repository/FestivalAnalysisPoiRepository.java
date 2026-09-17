package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisPoi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FestivalAnalysisPoiRepository extends JpaRepository<FestivalAnalysisPoi, Long> {
    List<FestivalAnalysisPoi> findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByDistanceMAsc(Long itemId);
}
