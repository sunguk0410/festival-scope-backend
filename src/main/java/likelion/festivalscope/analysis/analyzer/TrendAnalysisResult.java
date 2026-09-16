package likelion.festivalscope.analysis.analyzer;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record TrendAnalysisResult(
        List<MonthlyData> monthlyData,
        List<YearlyData> yearlyData,
        List<YearlyMetric> integratedYearly,
        Map<String, List<YearlyMetric>> keywordYearly,
        Map<String, List<MonthlyData>> previousYearAroundEventPeriod
) {
    public record MonthlyData(String keyword, YearMonth month, BigDecimal interestValue) {}
    public record YearlyData(String keyword, Integer year, BigDecimal interestValue) {}
    public record YearlyMetric(Integer year, BigDecimal yearlyInterest, BigDecimal growthRate) {}
}
