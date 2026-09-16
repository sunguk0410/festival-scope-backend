package likelion.festivalscope.plan.repository;

import likelion.festivalscope.plan.entity.FestivalPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalPlanRepository extends JpaRepository<FestivalPlan, Long> {
    List<FestivalPlan> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
