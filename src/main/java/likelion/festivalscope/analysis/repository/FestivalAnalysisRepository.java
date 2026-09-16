package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalAnalysisRepository extends JpaRepository<FestivalAnalysis, Long> {
    List<FestivalAnalysis> findAllByFestivalPlan_FestivalPlanIdOrderByCreatedAtDesc(Long festivalPlanId);
}
