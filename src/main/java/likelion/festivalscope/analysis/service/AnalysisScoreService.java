package likelion.festivalscope.analysis.service;

import likelion.festivalscope.analysis.analyzer.DemandFitAnalyzer;
import likelion.festivalscope.analysis.dto.response.DemandFitResponse;
import likelion.festivalscope.analysis.dto.response.InterpretationDecision;
import likelion.festivalscope.plan.entity.FestivalPlan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AnalysisScoreService {
    public BigDecimal score(FestivalPlan plan, InterpretationDecision decision, DemandFitAnalyzer.Result demandResult,
                            boolean demandItem) {
        if (hasMetric(decision, "gapRate")) return targetVisitorScore(value(decision, "gapRate"));
        if (hasMetric(decision, "latestGrowthRate")) return trendFitScore(value(decision, "latestGrowthRate"),
                value(decision, "decliningKeywordRate"), value(decision, "eventPeriodGap"));
        return demandItem ? demandFitScore(plan, demandResult, decision) : null;
    }

    public BigDecimal overall(BigDecimal target, BigDecimal trend, BigDecimal demand) {
        if (target == null || trend == null || demand == null) return null;
        return target.add(trend).add(demand).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }

    public String grade(BigDecimal totalScore) {
        if (totalScore == null) return null;
        if (totalScore.compareTo(BigDecimal.valueOf(85)) >= 0) return "A";
        if (totalScore.compareTo(BigDecimal.valueOf(70)) >= 0) return "B";
        if (totalScore.compareTo(BigDecimal.valueOf(55)) >= 0) return "C";
        if (totalScore.compareTo(BigDecimal.valueOf(40)) > 0) return "D";
        return "F";
    }

    private BigDecimal targetVisitorScore(BigDecimal gap) {
        if (gap == null) return null;
        if (gap.compareTo(BigDecimal.valueOf(100)) >= 0) return BigDecimal.valueOf(20);
        if (gap.compareTo(BigDecimal.valueOf(50)) >= 0) return BigDecimal.valueOf(40);
        if (gap.compareTo(BigDecimal.valueOf(20)) >= 0) return BigDecimal.valueOf(70);
        if (gap.compareTo(BigDecimal.valueOf(-20)) >= 0) return BigDecimal.valueOf(100);
        if (gap.compareTo(BigDecimal.valueOf(-50)) >= 0) return BigDecimal.valueOf(80);
        return BigDecimal.valueOf(60);
    }

    private BigDecimal trendFitScore(BigDecimal trend, BigDecimal decliningRate, BigDecimal eventGap) {
        if (trend == null || decliningRate == null || eventGap == null) return null;
        BigDecimal t = trend.compareTo(BigDecimal.valueOf(20)) >= 0 ? BigDecimal.valueOf(100) : trend.compareTo(BigDecimal.valueOf(5)) >= 0 ? BigDecimal.valueOf(80) : trend.compareTo(BigDecimal.valueOf(-5)) > 0 ? BigDecimal.valueOf(60) : trend.compareTo(BigDecimal.valueOf(-20)) > 0 ? BigDecimal.valueOf(40) : BigDecimal.valueOf(20);
        BigDecimal d = decliningRate.compareTo(BigDecimal.valueOf(40)) < 0 ? BigDecimal.valueOf(100) : decliningRate.compareTo(BigDecimal.valueOf(70)) < 0 ? BigDecimal.valueOf(60) : BigDecimal.valueOf(20);
        BigDecimal e = eventGap.compareTo(BigDecimal.valueOf(20)) >= 0 ? BigDecimal.valueOf(100) : eventGap.compareTo(BigDecimal.valueOf(-20)) > 0 ? BigDecimal.valueOf(60) : BigDecimal.valueOf(20);
        return t.multiply(BigDecimal.valueOf(0.40)).add(d.multiply(BigDecimal.valueOf(0.20))).add(e.multiply(BigDecimal.valueOf(0.40))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal demandFitScore(FestivalPlan plan, DemandFitAnalyzer.Result result, InterpretationDecision decision) {
        BigDecimal region = value(decision, "regionPercentile");
        BigDecimal month = value(decision, "monthPercentile");
        BigDecimal week = weekScore(plan, result == null ? null : result.seasonalDemand());
        BigDecimal access = accessibilityScore(result == null ? null : result.accessibility());
        if (region == null || month == null || week == null || access == null) return null;
        return region.add(month).add(week).add(access).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal weekScore(FestivalPlan plan, DemandFitResponse.SeasonalDemand seasonal) {
        if (plan.getStartDate() == null || seasonal == null || seasonal.recommendedWeek() == null || seasonal.weeklyDemand() == null) return null;
        int currentWeek = (plan.getStartDate().getDayOfMonth() - 1) / 7 + 1;
        DemandFitResponse.WeeklyDemand current = seasonal.weeklyDemand().stream().filter(week -> week.week() == currentWeek).findFirst().orElse(null);
        DemandFitResponse.WeeklyDemand recommended = seasonal.weeklyDemand().stream().filter(week -> week.week() == seasonal.recommendedWeek()).findFirst().orElse(null);
        if (current == null || recommended == null || current.averageDailyVisitors() == null || recommended.averageDailyVisitors() == null || recommended.averageDailyVisitors().signum() <= 0) return null;
        BigDecimal gap = current.averageDailyVisitors().subtract(recommended.averageDailyVisitors()).divide(recommended.averageDailyVisitors(), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return gap.compareTo(BigDecimal.valueOf(-10)) >= 0 ? BigDecimal.valueOf(100) : gap.compareTo(BigDecimal.valueOf(-25)) >= 0 ? BigDecimal.valueOf(70) : BigDecimal.valueOf(40);
    }

    private BigDecimal accessibilityScore(DemandFitResponse.Accessibility accessibility) {
        if (accessibility == null) return null;
        int available = 0;
        DemandFitResponse.Bus bus = accessibility.bus();
        if (bus != null && ((bus.routeCount() != null && bus.routeCount() > 0) || (bus.stopCount1km() != null && bus.stopCount1km() > 0))) available++;
        DemandFitResponse.Rail rail = accessibility.rail();
        if (rail != null && Boolean.TRUE.equals(rail.available()) && rail.nearestStationDistanceM() != null && rail.nearestStationDistanceM() <= 3000) available++;
        DemandFitResponse.Parking parking = accessibility.parking();
        if (parking != null && parking.parkingCount() != null && parking.parkingCount() > 0 && parking.parkingCapacity() != null && parking.parkingCapacity() > 0) available++;
        return available == 3 ? BigDecimal.valueOf(100) : available == 2 ? BigDecimal.valueOf(70) : BigDecimal.valueOf(40);
    }

    private BigDecimal value(InterpretationDecision decision, String key) {
        return decision.metrics().stream().filter(metric -> key.equals(metric.key())).map(metric -> metric.value())
                .filter(Number.class::isInstance).map(value -> value instanceof BigDecimal decimal ? decimal : BigDecimal.valueOf(((Number) value).doubleValue())).findFirst().orElse(null);
    }

    private boolean hasMetric(InterpretationDecision decision, String key) {
        return decision != null && decision.metrics() != null
                && decision.metrics().stream().anyMatch(metric -> key.equals(metric.key()));
    }
}
