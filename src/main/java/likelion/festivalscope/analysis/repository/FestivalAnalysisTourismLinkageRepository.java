package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisTourismLinkage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FestivalAnalysisTourismLinkageRepository extends JpaRepository<FestivalAnalysisTourismLinkage, Long> {
    Optional<FestivalAnalysisTourismLinkage> findByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId);
}
