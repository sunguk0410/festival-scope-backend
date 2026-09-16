package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.plan.entity.FestivalPlanProgram;

import java.util.List;

public interface TrendKeywordProvider {
    List<String> provide(FestivalPlan plan, List<FestivalPlanProgram> programs);
}
