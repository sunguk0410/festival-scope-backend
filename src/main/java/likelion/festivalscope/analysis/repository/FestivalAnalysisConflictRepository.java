package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FestivalAnalysisConflictRepository extends JpaRepository<FestivalAnalysisConflict, Long> {
    Optional<FestivalAnalysisConflict> findByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId);
}
