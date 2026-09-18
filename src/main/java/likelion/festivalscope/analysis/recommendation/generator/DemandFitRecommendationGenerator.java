package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.DemandType;
import likelion.festivalscope.analysis.entity.FestivalAnalysisAccessibility;
import likelion.festivalscope.analysis.entity.FestivalAnalysisDemand;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import likelion.festivalscope.common.util.RegionNameNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DemandFitRecommendationGenerator implements RecommendationGenerator {
    private static final String RECOMMENDATION_TYPE = "DEMAND_FIT";
    private static final int REGIONAL_ORDER = 30;
    private static final int WEEK_ORDER = 31;
    private static final int ACCESSIBILITY_ORDER = 32;

    @Override
    public AnalysisItemType supports() {
        return AnalysisItemType.DEMAND_FIT;
    }

    @Override
    public List<RecommendationDraft> generate(RecommendationContext context) {
        FestivalAnalysisItem item = context.item(supports());
        if (item == null || context.demand() == null) {
            return List.of();
        }

        List<RecommendationDraft> drafts = new ArrayList<>();
        regionalDemandDraft(context, item).ifPresent(drafts::add);
        weeklyDemandDraft(context, item).ifPresent(drafts::add);
        accessibilityDraft(context, item).ifPresent(drafts::add);
        return drafts;
    }

    private java.util.Optional<RecommendationDraft> regionalDemandDraft(
            RecommendationContext context, FestivalAnalysisItem item) {
        List<FestivalAnalysisDemand> rows = regionalRows(context);
        if (rows.isEmpty() || context.plan().getSigungu() == null || context.plan().getSido() == null) {
            return java.util.Optional.empty();
        }

        String targetType = adminType(context.plan().getSigungu());
        Map<String, Long> regionValues = rows.stream()
                .filter(row -> RegionNameNormalizer.sameSido(context.plan().getSido(), row.getSido()))
                .filter(row -> adminType(row.getSigungu()).equals(targetType))
                .filter(row -> row.getRegionCode() != null && row.getSigungu() != null
                        && row.getVisitorCount() != null)
                .collect(Collectors.groupingBy(row -> row.getRegionCode() + "|" + row.getSigungu(),
                        Collectors.summingLong(FestivalAnalysisDemand::getVisitorCount)));
        if (regionValues.isEmpty()) {
            return java.util.Optional.empty();
        }

        List<Map.Entry<String, Long>> ranked = regionValues.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();
        String targetKey = ranked.stream()
                .map(Map.Entry::getKey)
                .filter(key -> nameOf(key).equals(context.plan().getSigungu()))
                .findFirst()
                .orElse(null);
        if (targetKey == null || ranked.size() <= 1) {
            return java.util.Optional.empty();
        }

        int rank = indexOf(ranked, targetKey) + 1;
        BigDecimal percentile = percentile(rank, ranked.size());
        RecommendationPriority priority;
        if (percentile.compareTo(BigDecimal.valueOf(20)) < 0) {
            priority = RecommendationPriority.IMMEDIATE;
        } else if (percentile.compareTo(BigDecimal.valueOf(40)) < 0) {
            priority = RecommendationPriority.REVIEW;
        } else if (percentile.compareTo(BigDecimal.valueOf(70)) < 0) {
            return java.util.Optional.empty();
        } else {
            priority = RecommendationPriority.OPTIONAL;
        }

        String title;
        String content;
        if (priority == RecommendationPriority.IMMEDIATE) {
            title = "지역 관광수요를 보완할 유입 전략을 재검토하세요.";
            content = String.format("개최 지역의 관광수요는 비교 권역 기준 하위 %s%% 수준입니다. "
                            + "축제 자체의 프로그램·홍보를 통해 외부 방문객을 끌어올 수 있는 유입 전략을 강화하는 것을 권장합니다.",
                    format(percentile));
        } else if (priority == RecommendationPriority.REVIEW) {
            title = "지역 관광수요를 보완할 유입 전략을 검토하세요.";
            content = String.format("개최 지역의 관광수요는 비교 권역 기준 %s백분위 수준입니다. "
                            + "기본 관광수요가 높지 않은 만큼 홍보, 교통 연계 등 외부 방문객 유입 계획을 보완하는 것을 권장합니다.",
                    format(percentile));
        } else {
            title = "기존 지역 관광수요를 적극 활용하세요.";
            content = "개최 지역의 관광수요는 비교 권역 기준 상위 수준입니다. "
                    + "기존 관광객 흐름을 축제 방문으로 연결할 수 있도록 주변 관광자원과의 연계를 활용할 수 있습니다.";
        }
        return java.util.Optional.of(draft(item, priority, title, content, REGIONAL_ORDER));
    }

    private java.util.Optional<RecommendationDraft> weeklyDemandDraft(
            RecommendationContext context, FestivalAnalysisItem item) {
        if (context.plan().getStartDate() == null) {
            return java.util.Optional.empty();
        }
        List<FestivalAnalysisDemand> seasonalRows = context.demand().demandRows().stream()
                .filter(row -> row.getDemandType() == DemandType.SEASONAL
                        && row.getStatYear() != null && row.getStatMonth() != null
                        && row.getStatDay() != null && row.getVisitorCount() != null)
                .toList();
        if (seasonalRows.isEmpty()) {
            return java.util.Optional.empty();
        }

        int month = context.plan().getStartDate().getMonthValue();
        int endYear = LocalDate.now().getYear() - 1;
        List<WeekValue> weeks = weeklyValues(seasonalRows, month, endYear);
        List<WeekValue> available = weeks.stream()
                .filter(week -> week.average() != null && !week.referenceOnly())
                .sorted(Comparator.comparing(WeekValue::average).reversed())
                .toList();
        if (available.isEmpty()) {
            return java.util.Optional.empty();
        }

        Map<Integer, Integer> ranks = new HashMap<>();
        for (int index = 0; index < available.size(); index++) {
            ranks.put(available.get(index).week(), index + 1);
        }
        int currentWeek = Math.min(5, (context.plan().getStartDate().getDayOfMonth() - 1) / 7 + 1);
        Integer currentRank = ranks.get(currentWeek);
        WeekValue recommended = available.get(0);
        WeekValue current = weeks.stream().filter(week -> week.week() == currentWeek).findFirst().orElse(null);
        if (current == null || current.average() == null || currentRank == null
                || recommended.week() == currentWeek) {
            return java.util.Optional.empty();
        }

        int lowestRank = available.size();
        RecommendationPriority priority;
        if (currentRank == lowestRank) {
            priority = RecommendationPriority.IMMEDIATE;
        } else if (currentRank == 2) {
            priority = RecommendationPriority.OPTIONAL;
        } else if (currentRank == 3 || currentRank == 4) {
            priority = RecommendationPriority.REVIEW;
        } else {
            return java.util.Optional.empty();
        }

        String title;
        String content;
        if (priority == RecommendationPriority.IMMEDIATE) {
            title = "개최 주차를 재검토하세요.";
            content = String.format("현재 개최 예정인 %d주차의 평균 일 방문객은 %s명으로, 해당 월 내 관광수요가 가장 낮은 수준입니다. "
                            + "관광수요가 가장 높은 %d주차는 평균 %s명으로 확인되어, 일정 조정이 가능하다면 개최 주차 변경을 우선 검토하는 것을 권장합니다.",
                    currentWeek, format(current.average()), recommended.week(), format(recommended.average()));
        } else if (priority == RecommendationPriority.REVIEW) {
            title = "관광수요가 높은 주차로 일정 조정을 검토하세요.";
            content = String.format("현재 개최 예정인 %d주차의 평균 일 방문객은 %s명입니다. "
                            + "해당 월 내 관광수요가 가장 높은 %d주차는 평균 %s명으로 확인됩니다. "
                            + "일정 조정이 가능하다면 %d주차 개최를 검토하는 것을 권장합니다.",
                    currentWeek, format(current.average()), recommended.week(), format(recommended.average()), recommended.week());
        } else {
            title = "관광수요가 가장 높은 주차를 참고해보세요.";
            content = String.format("현재 개최 예정인 %d주차도 관광수요가 높은 편이지만, %d주차의 평균 일 방문객 %s명이 가장 높게 나타났습니다. "
                            + "세부 일정 조정이 가능하다면 %d주차 개최를 검토할 수 있습니다.",
                    currentWeek, recommended.week(), format(recommended.average()), recommended.week());
        }
        return java.util.Optional.of(draft(item, priority, title, content, WEEK_ORDER));
    }

    private java.util.Optional<RecommendationDraft> accessibilityDraft(
            RecommendationContext context, FestivalAnalysisItem item) {
        FestivalAnalysisAccessibility accessibility = context.demand().accessibility();
        if (accessibility == null) {
            return java.util.Optional.empty();
        }

        List<String> weakAreas = new ArrayList<>();
        if (isBusWeak(accessibility)) weakAreas.add("버스");
        if (isRailWeak(accessibility)) weakAreas.add("철도");
        if (isParkingWeak(accessibility)) weakAreas.add("주차");
        if (weakAreas.isEmpty()) {
            return java.util.Optional.empty();
        }

        RecommendationPriority priority = switch (weakAreas.size()) {
            case 1 -> RecommendationPriority.OPTIONAL;
            case 2 -> RecommendationPriority.REVIEW;
            default -> RecommendationPriority.IMMEDIATE;
        };
        String title;
        String content;
        if (priority == RecommendationPriority.IMMEDIATE) {
            title = "행사장 접근성 대책을 우선 보완하세요.";
            content = "대중교통과 주차 등 주요 접근 조건이 모두 부족한 상태입니다. "
                    + "셔틀버스, 임시주차장, 대중교통 연계 등 행사장 접근 대책을 우선 마련하는 것을 권장합니다.";
        } else if (priority == RecommendationPriority.REVIEW) {
            title = "행사장 접근성 보완 계획을 검토하세요.";
            content = String.format("주요 접근 조건 중 %s이(가) 부족한 것으로 확인됩니다. "
                            + "셔틀버스 운영이나 임시주차장 확보 등 부족한 이동수단을 보완하는 것을 권장합니다.",
                    String.join(", ", weakAreas));
        } else {
            title = "부족한 접근 조건을 보완해보세요.";
            content = String.format("전체적인 접근성은 확보되어 있으나 %s은(는) 상대적으로 부족합니다. "
                            + "방문객 편의를 위해 해당 접근수단을 추가 보완할 수 있습니다.",
                    String.join(", ", weakAreas));
        }
        return java.util.Optional.of(draft(item, priority, title, content, ACCESSIBILITY_ORDER));
    }

    private List<FestivalAnalysisDemand> regionalRows(RecommendationContext context) {
        return context.demand().demandRows().stream()
                .filter(row -> row.getDemandType() == DemandType.REGIONAL)
                .toList();
    }

    private List<WeekValue> weeklyValues(List<FestivalAnalysisDemand> rows, int month, int endYear) {
        List<WeekValue> values = new ArrayList<>();
        for (int week = 1; week <= 5; week++) {
            int start = (week - 1) * 7 + 1;
            int end = Math.min(week * 7, YearMonth.of(endYear, month).lengthOfMonth());
            int days = Math.max(0, end - start + 1);
            boolean referenceOnly = days < 3;
            List<FestivalAnalysisDemand> weekRows = rows.stream()
                    .filter(row -> row.getStatYear() == endYear && row.getStatMonth() == month
                            && row.getStatDay() >= start && row.getStatDay() <= end)
                    .toList();
            BigDecimal average = weekRows.isEmpty() ? null
                    : BigDecimal.valueOf(weekRows.stream().mapToLong(FestivalAnalysisDemand::getVisitorCount).sum())
                    .divide(BigDecimal.valueOf(weekRows.size()), 2, RoundingMode.HALF_UP);
            values.add(new WeekValue(week, average, referenceOnly));
        }
        return values;
    }

    private boolean isBusWeak(FestivalAnalysisAccessibility accessibility) {
        return (accessibility.getBusStopCount1km() != null && accessibility.getBusStopCount1km() == 0)
                || (accessibility.getBusRouteCount() != null && accessibility.getBusRouteCount() == 0);
    }

    private boolean isRailWeak(FestivalAnalysisAccessibility accessibility) {
        return Boolean.FALSE.equals(accessibility.getRailAvailable())
                || (accessibility.getNearestStationDistanceM() != null
                && accessibility.getNearestStationDistanceM() > 1_000);
    }

    private boolean isParkingWeak(FestivalAnalysisAccessibility accessibility) {
        return (accessibility.getParkingCount() != null && accessibility.getParkingCount() == 0)
                || (accessibility.getParkingCapacity() != null && accessibility.getParkingCapacity() < 100);
    }

    private RecommendationDraft draft(FestivalAnalysisItem item, RecommendationPriority priority,
                                      String title, String content, int displayOrder) {
        return new RecommendationDraft(RECOMMENDATION_TYPE, priority, title, content, displayOrder, item);
    }

    private String adminType(String name) {
        if (name == null) return "";
        if (name.endsWith("군")) return "군";
        if (name.endsWith("시")) return "시";
        if (name.endsWith("구")) return "구";
        return name;
    }

    private String nameOf(String key) {
        int index = key.indexOf('|');
        return index < 0 ? key : key.substring(index + 1);
    }

    private int indexOf(List<Map.Entry<String, Long>> values, String key) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).getKey().equals(key)) return i;
        }
        return -1;
    }

    private BigDecimal percentile(int rank, int total) {
        return BigDecimal.valueOf((total - rank + 1) * 100D / total)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private record WeekValue(int week, BigDecimal average, boolean referenceOnly) {
    }
}
