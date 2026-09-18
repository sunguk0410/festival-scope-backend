package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.FestivalAnalysisSimilar;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.entity.TargetVisitorComparisonType;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import likelion.festivalscope.plan.entity.FestivalStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class TargetVisitorRecommendationGenerator implements RecommendationGenerator {
    private static final int DISPLAY_ORDER = 10;
    private static final String RECOMMENDATION_TYPE = "TARGET_VISITOR";

    @Override
    public AnalysisItemType supports() {
        return AnalysisItemType.TARGET_VISITOR;
    }

    @Override
    public List<RecommendationDraft> generate(RecommendationContext context) {
        FestivalAnalysisItem item = context.item(supports());
        RecommendationContext.TargetVisitorData targetVisitor = context.targetVisitor();
        if (item == null || targetVisitor == null || targetVisitor.snapshot() == null
                || targetVisitor.snapshot().getTargetVisitorCount() == null
                || targetVisitor.snapshot().getTargetVisitorCount() <= 0) {
            return List.of();
        }

        if (context.plan().getFestivalStatus() != FestivalStatus.EXISTING
                && context.plan().getFestivalStatus() != FestivalStatus.NEW) {
            return List.of();
        }

        List<FestivalAnalysisSimilar> similarFestivals = targetVisitor.similarFestivals();
        BigDecimal similarMedian = median(similarFestivals, TargetVisitorComparisonType.SIMILAR_FESTIVAL);
        if (similarMedian == null || similarMedian.signum() == 0) {
            return List.of();
        }

        BigDecimal target = BigDecimal.valueOf(targetVisitor.snapshot().getTargetVisitorCount());
        BigDecimal similarGap = gapRate(target, similarMedian);

        if (context.plan().getFestivalStatus() == FestivalStatus.EXISTING) {
            BigDecimal historyMedian = median(similarFestivals, TargetVisitorComparisonType.SAME_FESTIVAL);
            if (historyMedian == null || historyMedian.signum() == 0) {
                return List.of();
            }

            BigDecimal historyGap = gapRate(target, historyMedian);
            RecommendationPriority priority = priorityFor(historyGap);
            if (priority == null) {
                return List.of();
            }
            return List.of(new RecommendationDraft(
                    RECOMMENDATION_TYPE,
                    priority,
                    titleForExisting(priority),
                    contentForExisting(priority, target, historyMedian, historyGap, similarMedian, similarGap),
                    DISPLAY_ORDER,
                    item));
        }

        RecommendationPriority priority = priorityFor(similarGap);
        if (priority == null) {
            return List.of();
        }
        return List.of(new RecommendationDraft(
                RECOMMENDATION_TYPE,
                priority,
                titleForNew(priority),
                contentForNew(priority, target, similarMedian, similarGap),
                DISPLAY_ORDER,
                item));
    }

    private BigDecimal median(List<FestivalAnalysisSimilar> rows,
                              TargetVisitorComparisonType comparisonType) {
        List<Long> values = rows.stream()
                .filter(row -> row.getComparisonType() == comparisonType)
                .map(FestivalAnalysisSimilar::getVisitorCount)
                .filter(value -> value != null && value >= 0)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return BigDecimal.valueOf(values.get(middle));
        }
        return BigDecimal.valueOf(values.get(middle - 1))
                .add(BigDecimal.valueOf(values.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal gapRate(BigDecimal target, BigDecimal median) {
        return target.subtract(median)
                .divide(median, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private RecommendationPriority priorityFor(BigDecimal gap) {
        if (gap.compareTo(BigDecimal.valueOf(50)) >= 0) return RecommendationPriority.IMMEDIATE;
        if (gap.compareTo(BigDecimal.valueOf(20)) >= 0) return RecommendationPriority.REVIEW;
        if (gap.compareTo(BigDecimal.valueOf(-20)) >= 0) return null;
        if (gap.compareTo(BigDecimal.valueOf(-50)) >= 0) return RecommendationPriority.OPTIONAL;
        return RecommendationPriority.REVIEW;
    }

    private String titleForNew(RecommendationPriority priority) {
        return switch (priority) {
            case IMMEDIATE -> "목표 방문객 규모를 재검토하세요.";
            case REVIEW -> "목표 방문객의 달성 근거를 보완하세요.";
            case OPTIONAL -> "목표 방문객 상향 가능성을 검토하세요.";
        };
    }

    private String titleForExisting(RecommendationPriority priority) {
        return switch (priority) {
            case IMMEDIATE -> "목표 방문객 규모를 재검토하세요.";
            case REVIEW -> "목표 방문객의 성장 근거를 보완하세요.";
            case OPTIONAL -> "목표 방문객 상향 가능성을 검토하세요.";
        };
    }

    private String contentForNew(RecommendationPriority priority, BigDecimal target,
                                 BigDecimal similarMedian, BigDecimal similarGap) {
        String targetText = format(target);
        String medianText = format(similarMedian);
        if (priority == RecommendationPriority.IMMEDIATE) {
            return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 %s%% 높게 설정되어 있습니다. "
                            + "유사 축제 실적을 고려해 목표 수준을 조정하는 것을 권장합니다.",
                    targetText, medianText, format(similarGap));
        }
        if (priority == RecommendationPriority.REVIEW) {
            return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 %s%% 높게 설정되어 있습니다. "
                            + "현재 목표를 유지하려면 목표 달성을 뒷받침할 근거를 보완하는 것을 권장합니다.",
                    targetText, medianText, format(similarGap));
        }
        return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 %s%% 낮게 설정되어 있습니다. "
                        + "행사 규모를 고려해 목표 상향 가능성을 검토할 수 있습니다.",
                targetText, medianText, format(similarGap.abs()));
    }

    private String contentForExisting(RecommendationPriority priority, BigDecimal target,
                                      BigDecimal historyMedian, BigDecimal historyGap,
                                      BigDecimal similarMedian, BigDecimal similarGap) {
        String targetText = format(target);
        String historyText = format(historyMedian);
        String similarText = format(similarMedian);
        if (priority == RecommendationPriority.IMMEDIATE) {
            return String.format("현재 목표 %s명은 동일 축제 과거 방문객 중앙값 %s명보다 %s%% 높게 설정되어 있습니다. "
                            + "유사 축제 방문객 중앙값은 %s명으로, 현재 목표는 유사 축제 대비 %s%% 수준입니다. "
                            + "과거 실적을 고려해 목표 수준을 조정하는 것을 권장합니다.",
                    targetText, historyText, format(historyGap), similarText, format(similarGap));
        }
        if (priority == RecommendationPriority.REVIEW) {
            return String.format("현재 목표 %s명은 동일 축제 과거 방문객 중앙값 %s명보다 %s%% 높게 설정되어 있습니다. "
                            + "유사 축제 방문객 중앙값은 %s명으로, 현재 목표는 유사 축제 대비 %s%% 수준입니다. "
                            + "현재 목표를 유지하려면 과거 실적 대비 성장 근거를 보완하는 것을 권장합니다.",
                    targetText, historyText, format(historyGap), similarText, format(similarGap));
        }
        return String.format("현재 목표 %s명은 동일 축제 과거 방문객 중앙값 %s명보다 %s%% 낮게 설정되어 있습니다. "
                        + "유사 축제 방문객 중앙값은 %s명입니다. 행사 규모를 고려해 목표 상향 가능성을 검토할 수 있습니다.",
                targetText, historyText, format(historyGap.abs()), similarText);
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
