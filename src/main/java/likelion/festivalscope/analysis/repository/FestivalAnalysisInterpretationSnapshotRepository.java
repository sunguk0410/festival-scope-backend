package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisInterpretationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FestivalAnalysisInterpretationSnapshotRepository
        extends JpaRepository<FestivalAnalysisInterpretationSnapshot, Long> {
    Optional<FestivalAnalysisInterpretationSnapshot> findByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId);
}
