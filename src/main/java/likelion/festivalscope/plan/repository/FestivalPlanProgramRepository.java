package likelion.festivalscope.plan.repository;

import likelion.festivalscope.plan.entity.FestivalPlanProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalPlanProgramRepository extends JpaRepository<FestivalPlanProgram, Long> {
    List<FestivalPlanProgram> findAllByFestivalPlan_FestivalPlanId(Long festivalPlanId);
}
