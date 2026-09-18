package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.external.naver.NaverDataLabClient;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.plan.entity.FestivalPlanProgram;
import likelion.festivalscope.plan.repository.FestivalPlanProgramRepository;
import likelion.festivalscope.trend.service.TrendKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class TrendFitAnalyzer {
    private final FestivalPlanProgramRepository festivalPlanProgramRepository;
    private final TrendKeywordService trendKeywordService;
    private final NaverDataLabClient naverDataLabClient;

    public TrendAnalysisResult analyze(FestivalPlan plan) {
        List<FestivalPlanProgram> programs = festivalPlanProgramRepository
                .findAllByFestivalPlan_FestivalPlanId(plan.getFestivalPlanId());
        List<String> programNames = programs.stream().map(FestivalPlanProgram::getProgramName).toList();
        List<String> keywords = trendKeywordService.extractKeywords(plan.getFestivalName(), programNames);
        if (keywords.isEmpty()) {
            throw new AnalysisExecutionException("TREND_FIT 분석에 사용할 키워드가 없습니다.");
        }

        int currentYear = Year.now().getValue();
        List<Integer> recentYears = IntStream.rangeClosed(currentYear - 3, currentYear - 1).boxed().toList();
        YearMonth historicalStart = YearMonth.of(recentYears.get(0), 1);
        YearMonth historicalEnd = YearMonth.of(recentYears.get(recentYears.size() - 1), 12);
        Optional<Period> eventPeriod = createPreviousYearEventPeriod(plan.getStartDate());
        YearMonth requestStart = eventPeriod.map(Period::start).map(month -> month.isBefore(historicalStart) ? month : historicalStart)
                .orElse(historicalStart);
        YearMonth requestEnd = eventPeriod.map(Period::end).map(month -> month.isAfter(historicalEnd) ? month : historicalEnd)
                .orElse(historicalEnd);

        // DataLab은 미래 날짜를 조회할 수 없으므로 행사 예정일에서 계산한 기간을 현재 월까지만 제한한다.
        YearMonth latestAllowedMonth = YearMonth.from(LocalDate.now());
        if (requestEnd.isAfter(latestAllowedMonth)) {
            requestEnd = latestAllowedMonth;
        }
        // 행사 예정일이 너무 미래라 요청 시작월도 현재보다 뒤에 남는 경우,
        // 최근 3개년 전체 구간으로 되돌려 유효한 날짜 범위를 만든다.
        if (requestStart.isAfter(requestEnd)) {
            requestStart = historicalStart;
        }

        LocalDate requestEndDate = requestEnd.atEndOfMonth();
        if (requestEndDate.isAfter(LocalDate.now())) {
            requestEndDate = LocalDate.now();
        }
        Map<String, List<NaverDataLabClient.DataPoint>> dataByKeyword = naverDataLabClient.search(
                keywords, requestStart.atDay(1), requestEndDate);

        List<TrendAnalysisResult.MonthlyData> monthlyData = new ArrayList<>();
        Map<String, List<TrendAnalysisResult.YearlyMetric>> keywordYearly = new LinkedHashMap<>();
        List<TrendAnalysisResult.YearlyData> yearlyData = new ArrayList<>();

        for (String keyword : keywords) {
            Map<YearMonth, BigDecimal> points = pointsFor(dataByKeyword, keyword).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            NaverDataLabClient.DataPoint::month,
                            NaverDataLabClient.DataPoint::ratio,
                            (first, second) -> first));
            for (YearMonth month : monthsBetween(requestStart, requestEnd)) {
                BigDecimal value = points.get(month);
                if (value != null) monthlyData.add(new TrendAnalysisResult.MonthlyData(keyword, month, value));
            }

            List<TrendAnalysisResult.YearlyMetric> yearlyMetrics = new ArrayList<>();
            for (Integer year : recentYears) {
                BigDecimal interest = average(monthsBetween(YearMonth.of(year, 1), YearMonth.of(year, 12))
                        .stream().map(points::get).filter(Objects::nonNull).toList());
                BigDecimal growthRate = yearlyMetrics.isEmpty() ? null
                        : calculateGrowth(yearlyMetrics.get(yearlyMetrics.size() - 1).yearlyInterest(), interest);
                yearlyMetrics.add(new TrendAnalysisResult.YearlyMetric(year, interest, growthRate));
                yearlyData.add(new TrendAnalysisResult.YearlyData(keyword, year, interest));
            }
            keywordYearly.put(keyword, yearlyMetrics);
        }

        List<TrendAnalysisResult.YearlyMetric> integratedYearly = new ArrayList<>();
        for (Integer year : recentYears) {
            BigDecimal interest = average(keywordYearly.values().stream()
                    .map(metrics -> metrics.stream().filter(metric -> metric.year().equals(year)).findFirst()
                            .map(TrendAnalysisResult.YearlyMetric::yearlyInterest).orElse(null))
                    .filter(Objects::nonNull).toList());
            BigDecimal growthRate = integratedYearly.isEmpty() ? null
                    : calculateGrowth(integratedYearly.get(integratedYearly.size() - 1).yearlyInterest(), interest);
            integratedYearly.add(new TrendAnalysisResult.YearlyMetric(year, interest, growthRate));
        }

        Map<String, List<TrendAnalysisResult.MonthlyData>> aroundEvent = new LinkedHashMap<>();
        eventPeriod.ifPresent(period -> keywords.forEach(keyword -> {
            Map<YearMonth, BigDecimal> points = pointsFor(dataByKeyword, keyword).stream()
                    .collect(java.util.stream.Collectors.toMap(
                            NaverDataLabClient.DataPoint::month,
                            NaverDataLabClient.DataPoint::ratio,
                            (first, second) -> first));
            List<TrendAnalysisResult.MonthlyData> values = monthsBetween(period.start(), period.end()).stream()
                    .map(month -> new TrendAnalysisResult.MonthlyData(keyword, month, points.get(month)))
                    .toList();
            aroundEvent.put(keyword, values);
        }));

        return new TrendAnalysisResult(monthlyData, yearlyData, integratedYearly, keywordYearly, aroundEvent);
    }

    private List<NaverDataLabClient.DataPoint> pointsFor(
            Map<String, List<NaverDataLabClient.DataPoint>> dataByKeyword, String keyword) {
        return dataByKeyword.getOrDefault(keyword,
                dataByKeyword.getOrDefault(keyword.toLowerCase(Locale.ROOT), List.of()));
    }
    private Optional<Period> createPreviousYearEventPeriod(LocalDate startDate) {
        if (startDate == null) return Optional.empty();
        YearMonth eventMonth = YearMonth.from(startDate).minusYears(1);
        return Optional.of(new Period(eventMonth.minusMonths(3), eventMonth.plusMonths(3)));
    }

    private List<YearMonth> monthsBetween(YearMonth start, YearMonth end) {
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) months.add(month);
        return months;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGrowth(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private record Period(YearMonth start, YearMonth end) {}
}
