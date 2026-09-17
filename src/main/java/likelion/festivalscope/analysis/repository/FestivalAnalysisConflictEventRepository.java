package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FestivalAnalysisConflictEventRepository extends JpaRepository<FestivalAnalysisConflictEvent, Long> {
    List<FestivalAnalysisConflictEvent> findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByEventYearAscStartDateAsc(Long itemId);
}
