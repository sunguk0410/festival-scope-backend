package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisWeatherRiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FestivalAnalysisWeatherRiskSnapshotRepository extends JpaRepository<FestivalAnalysisWeatherRiskSnapshot, Long> {
    Optional<FestivalAnalysisWeatherRiskSnapshot> findByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId);
}
