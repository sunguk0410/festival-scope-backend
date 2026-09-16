package likelion.festivalscope.plan.repository;

import likelion.festivalscope.plan.entity.FestivalPlanTheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalPlanThemeRepository extends JpaRepository<FestivalPlanTheme, Long> {
    List<FestivalPlanTheme> findAllByFestivalPlan_FestivalPlanId(Long festivalPlanId);
}
