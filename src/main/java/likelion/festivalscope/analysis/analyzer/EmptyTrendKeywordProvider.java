package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.plan.entity.FestivalPlanProgram;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmptyTrendKeywordProvider implements TrendKeywordProvider {
    @Override
    public List<String> provide(FestivalPlan plan, List<FestivalPlanProgram> programs) {
        return List.of();
    }
}
