package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisAccessibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FestivalAnalysisAccessibilityRepository extends JpaRepository<FestivalAnalysisAccessibility, Long> {
    Optional<FestivalAnalysisAccessibility> findByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId);
}