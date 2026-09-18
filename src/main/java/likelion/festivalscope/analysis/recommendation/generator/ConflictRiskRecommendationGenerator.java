package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysisConflict;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConflictRiskRecommendationGenerator implements RecommendationGenerator {
    private static final String RECOMMENDATION_TYPE = "CONFLICT_RISK";
    private static final int DISPLAY_ORDER = 40;

    @Override
    public AnalysisItemType supports() {
        return AnalysisItemType.CONFLICT_RISK;
    }

    @Override
    public List<RecommendationDraft> generate(RecommendationContext context) {
        FestivalAnalysisItem item = context.item(supports());
        FestivalAnalysisConflict conflict = context.conflict();
        if (item == null || conflict == null || conflict.getHistoricalSamePeriodCount() == null) {
            return List.of();
        }

        int historicalCount = conflict.getHistoricalSamePeriodCount();
        RecommendationPriority priority = priorityFor(historicalCount);
        if (priority == null) {
            return List.of();
        }

        String title;
        String content;
        if (priority == RecommendationPriority.IMMEDIATE) {
            title = "개최 일정 조정을 우선 검토하세요.";
            content = String.format("최근 5년간 동일 시기에 주변 행사가 %d건 확인되었습니다. "
                            + "동일 시기의 행사 집중이 반복된 만큼 방문객 및 교통 수요 충돌 가능성을 고려해 개최 일정 조정을 우선 검토하는 것을 권장합니다.",
                    historicalCount);
        } else if (priority == RecommendationPriority.REVIEW) {
            title = "동일 시기 행사 중복 가능성을 검토하세요.";
            content = String.format("최근 5년간 동일 시기에 주변 행사가 %d건 확인되었습니다. "
                            + "현재 일정을 유지한다면 교통·주차·홍보 계획을 사전에 보완하는 것을 권장합니다.",
                    historicalCount);
        } else {
            title = "과거 동일 시기 행사를 운영 계획에 참고하세요.";
            content = String.format("최근 5년간 동일 시기에 주변 행사 %d건이 확인되었습니다. "
                            + "일정 변경이 필요한 수준은 아니지만 운영 및 홍보 계획 수립 시 해당 행사 이력을 함께 고려할 수 있습니다.",
                    historicalCount);
        }

        return List.of(new RecommendationDraft(
                RECOMMENDATION_TYPE,
                priority,
                title,
                content,
                DISPLAY_ORDER,
                item));
    }

    private RecommendationPriority priorityFor(int historicalCount) {
        if (historicalCount >= 3) return RecommendationPriority.IMMEDIATE;
        if (historicalCount == 2) return RecommendationPriority.REVIEW;
        if (historicalCount == 1) return RecommendationPriority.OPTIONAL;
        return null;
    }
}
