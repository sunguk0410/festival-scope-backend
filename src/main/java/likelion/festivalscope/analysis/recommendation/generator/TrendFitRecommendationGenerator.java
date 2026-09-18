package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTrendKeyword;
import likelion.festivalscope.analysis.entity.PeriodType;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TrendFitRecommendationGenerator implements RecommendationGenerator {
    private static final String RECOMMENDATION_TYPE = "TREND_FIT";
    private static final int RECENT_TREND_ORDER = 20;
    private static final int EVENT_PERIOD_ORDER = 21;
    private static final int KEYWORD_TREND_ORDER = 22;

    @Override
    public AnalysisItemType supports() {
        return AnalysisItemType.TREND_FIT;
    }

    @Override
    public List<RecommendationDraft> generate(RecommendationContext context) {
        FestivalAnalysisItem item = context.item(supports());
        if (item == null || context.trendKeywords() == null || context.trendKeywords().isEmpty()) {
            return List.of();
        }

        List<RecommendationDraft> drafts = new ArrayList<>();
        recentTrendDraft(context, item).ifPresent(drafts::add);
        eventPeriodDraft(context, item).ifPresent(drafts::add);
        keywordTrendDraft(context, item).ifPresent(drafts::add);
        return drafts;
    }

    private java.util.Optional<RecommendationDraft> recentTrendDraft(
            RecommendationContext context, FestivalAnalysisItem item) {
        List<FestivalAnalysisTrendKeyword> yearly = rows(context, PeriodType.YEARLY);
        Map<Integer, List<BigDecimal>> valuesByYear = yearly.stream()
                .filter(row -> row.getPeriodYear() != null && row.getInterestValue() != null)
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getPeriodYear,
                        LinkedHashMap::new,
                        Collectors.mapping(FestivalAnalysisTrendKeyword::getInterestValue, Collectors.toList())));
        List<Integer> years = valuesByYear.keySet().stream().sorted().toList();
        if (years.size() < 2) {
            return java.util.Optional.empty();
        }

        BigDecimal previous = average(valuesByYear.get(years.get(years.size() - 2)));
        BigDecimal current = average(valuesByYear.get(years.get(years.size() - 1)));
        BigDecimal growth = growthRate(previous, current);
        if (growth == null) {
            return java.util.Optional.empty();
        }

        RecommendationPriority priority = recentTrendPriority(growth);
        if (priority == null) {
            return java.util.Optional.empty();
        }
        String title;
        String content;
        if (priority == RecommendationPriority.IMMEDIATE) {
            title = "핵심 콘텐츠 구성을 재검토하세요.";
            content = String.format("축제 핵심 콘텐츠의 최근 검색 관심도가 전년 대비 %s%% 감소했습니다. "
                            + "관심이 하락하는 콘텐츠 비중을 조정하고 새로운 관심 요소를 보완하는 것을 권장합니다.",
                    format(growth.abs()));
        } else if (priority == RecommendationPriority.REVIEW) {
            title = "최근 관심 하락 원인을 검토하세요.";
            content = String.format("축제 핵심 콘텐츠의 최근 검색 관심도가 전년 대비 %s%% 감소했습니다. "
                            + "현재 콘텐츠 구성을 유지할 경우 관심을 보완할 프로그램이나 홍보 요소를 함께 검토하는 것을 권장합니다.",
                    format(growth.abs()));
        } else {
            title = "상승 중인 관심도를 기획에 적극 활용하세요.";
            content = String.format("축제 핵심 콘텐츠의 최근 검색 관심도가 전년 대비 %s%% 증가했습니다. "
                            + "관심이 상승하는 콘텐츠를 핵심 프로그램이나 홍보 메시지에 적극 활용할 수 있습니다.",
                    format(growth));
        }
        return java.util.Optional.of(draft(item, priority, title, content, RECENT_TREND_ORDER));
    }

    private java.util.Optional<RecommendationDraft> eventPeriodDraft(
            RecommendationContext context, FestivalAnalysisItem item) {
        if (context.plan().getStartDate() == null) {
            return java.util.Optional.empty();
        }

        YearMonth eventMonth = YearMonth.from(context.plan().getStartDate()).minusYears(1);
        Map<String, Map<YearMonth, BigDecimal>> valuesByKeyword = monthlyRows(context).stream()
                .filter(row -> row.getKeyword() != null && row.getPeriodYear() != null
                        && row.getPeriodMonth() != null && row.getInterestValue() != null)
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getKeyword,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                row -> YearMonth.of(row.getPeriodYear(), row.getPeriodMonth()),
                                FestivalAnalysisTrendKeyword::getInterestValue,
                                (first, second) -> first,
                                LinkedHashMap::new)));

        List<BigDecimal> gaps = new ArrayList<>();
        for (Map<YearMonth, BigDecimal> values : valuesByKeyword.values()) {
            BigDecimal eventInterest = values.get(eventMonth);
            List<BigDecimal> surrounding = new ArrayList<>();
            for (int offset = -3; offset <= 3; offset++) {
                if (offset == 0) {
                    continue;
                }
                BigDecimal value = values.get(eventMonth.plusMonths(offset));
                if (value != null) {
                    surrounding.add(value);
                }
            }
            if (eventInterest == null || surrounding.isEmpty()) {
                continue;
            }
            BigDecimal surroundingAverage = average(surrounding);
            BigDecimal gap = growthRate(surroundingAverage, eventInterest);
            if (gap != null) {
                gaps.add(gap);
            }
        }

        if (gaps.isEmpty()) {
            return java.util.Optional.empty();
        }
        BigDecimal averageGap = average(gaps);
        RecommendationPriority priority = eventPeriodPriority(averageGap);
        if (priority == null) {
            return java.util.Optional.empty();
        }

        String title = priority == RecommendationPriority.IMMEDIATE
                ? "개최 시기와 콘텐츠 구성을 재검토하세요."
                : "개최 시기의 콘텐츠 적합성을 보완하세요.";
        String content = priority == RecommendationPriority.IMMEDIATE
                ? String.format("핵심 콘텐츠의 개최 시기 검색 관심도가 주변 시기 평균보다 %s%% 낮습니다. "
                        + "현재 시기를 유지한다면 계절에 더 적합한 프로그램을 추가하는 것을 권장합니다.",
                format(averageGap.abs()))
                : String.format("핵심 콘텐츠의 개최 시기 검색 관심도가 주변 시기 평균보다 %s%% 낮습니다. "
                        + "현재 개최 시기를 유지할 경우 해당 시기에 관심을 끌 수 있는 프로그램 보완을 권장합니다.",
                format(averageGap.abs()));
        return java.util.Optional.of(draft(item, priority, title, content, EVENT_PERIOD_ORDER));
    }

    private java.util.Optional<RecommendationDraft> keywordTrendDraft(
            RecommendationContext context, FestivalAnalysisItem item) {
        Map<String, List<FestivalAnalysisTrendKeyword>> rowsByKeyword = rows(context, PeriodType.YEARLY).stream()
                .filter(row -> row.getKeyword() != null && row.getPeriodYear() != null
                        && row.getInterestValue() != null)
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getKeyword,
                        LinkedHashMap::new, Collectors.toList()));
        if (rowsByKeyword.isEmpty()) {
            return java.util.Optional.empty();
        }

        List<String> decliningKeywords = new ArrayList<>();
        int validLatestGrowthCount = 0;
        for (Map.Entry<String, List<FestivalAnalysisTrendKeyword>> entry : rowsByKeyword.entrySet()) {
            List<FestivalAnalysisTrendKeyword> rows = entry.getValue().stream()
                    .sorted(Comparator.comparing(FestivalAnalysisTrendKeyword::getPeriodYear))
                    .toList();
            if (rows.size() < 2) {
                continue;
            }
            FestivalAnalysisTrendKeyword previous = rows.get(rows.size() - 2);
            FestivalAnalysisTrendKeyword current = rows.get(rows.size() - 1);
            BigDecimal growth = growthRate(previous.getInterestValue(), current.getInterestValue());
            if (growth == null) {
                continue;
            }
            validLatestGrowthCount++;
            if (growth.compareTo(BigDecimal.valueOf(-20)) <= 0) {
                decliningKeywords.add(entry.getKey());
            }
        }

        if (validLatestGrowthCount == 0) {
            return java.util.Optional.empty();
        }
        RecommendationPriority priority = switch (decliningKeywords.size()) {
            case 0 -> null;
            case 1 -> RecommendationPriority.OPTIONAL;
            case 2 -> RecommendationPriority.REVIEW;
            default -> RecommendationPriority.IMMEDIATE;
        };
        if (priority == null) {
            return java.util.Optional.empty();
        }

        BigDecimal ratio = BigDecimal.valueOf(decliningKeywords.size() * 100L)
                .divide(BigDecimal.valueOf(rowsByKeyword.size()), 2, RoundingMode.HALF_UP);
        String keywords = String.join(", ", decliningKeywords);
        String title;
        String content;
        if (priority == RecommendationPriority.IMMEDIATE) {
            title = "핵심 프로그램 구성을 재검토하세요.";
            content = String.format("분석 키워드의 %s%%에서 최근 검색 관심도가 20%% 이상 감소했습니다. "
                            + "특히 %s의 관심 하락이 확인되어 해당 콘텐츠 비중을 조정하거나 새로운 프로그램으로 보완하는 것을 권장합니다.",
                    format(ratio), keywords);
        } else if (priority == RecommendationPriority.REVIEW) {
            title = "하락 중인 콘텐츠 비중을 검토하세요.";
            content = String.format("분석 키워드의 %s%%에서 최근 검색 관심도가 20%% 이상 감소했습니다. "
                            + "%s 중심의 프로그램 비중을 검토하고 관심이 유지되는 콘텐츠와의 조합을 권장합니다.",
                    format(ratio), keywords);
        } else {
            title = "일부 하락 콘텐츠의 보완을 검토하세요.";
            content = String.format("일부 핵심 키워드에서 최근 관심도 하락이 확인되었습니다. "
                            + "%s의 비중을 조정하거나 관심이 높은 콘텐츠를 함께 구성하는 방안을 검토할 수 있습니다.",
                    keywords);
        }
        return java.util.Optional.of(draft(item, priority, title, content, KEYWORD_TREND_ORDER));
    }

    private List<FestivalAnalysisTrendKeyword> rows(RecommendationContext context, PeriodType periodType) {
        return context.trendKeywords().stream()
                .filter(row -> row.getPeriodType() == periodType)
                .toList();
    }

    private List<FestivalAnalysisTrendKeyword> monthlyRows(RecommendationContext context) {
        return rows(context, PeriodType.MONTHLY);
    }

    private RecommendationDraft draft(FestivalAnalysisItem item, RecommendationPriority priority,
                                      String title, String content, int displayOrder) {
        return new RecommendationDraft(RECOMMENDATION_TYPE, priority, title, content, displayOrder, item);
    }

    private RecommendationPriority recentTrendPriority(BigDecimal growth) {
        if (growth.compareTo(BigDecimal.valueOf(-30)) <= 0) return RecommendationPriority.IMMEDIATE;
        if (growth.compareTo(BigDecimal.valueOf(-10)) <= 0) return RecommendationPriority.REVIEW;
        if (growth.compareTo(BigDecimal.valueOf(10)) >= 0) return RecommendationPriority.OPTIONAL;
        return null;
    }

    private RecommendationPriority eventPeriodPriority(BigDecimal gap) {
        if (gap.compareTo(BigDecimal.valueOf(-40)) <= 0) return RecommendationPriority.IMMEDIATE;
        if (gap.compareTo(BigDecimal.valueOf(-20)) <= 0) return RecommendationPriority.REVIEW;
        return null;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal growthRate(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null || previous.signum() == 0) return null;
        return current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
