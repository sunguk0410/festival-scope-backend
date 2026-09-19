package likelion.festivalscope.analysis.weather;

import likelion.festivalscope.external.weather.dto.AsosDailyWeatherDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherStatisticsCalculatorTest {
    private final WeatherStatisticsCalculator calculator = new WeatherStatisticsCalculator(
            BigDecimal.ZERO, BigDecimal.valueOf(30), BigDecimal.valueOf(-10), BigDecimal.valueOf(10));

    @Test
    void calculatesOccurrenceAndDayRatesWithMissingValuesExcluded() {
        AsosDailyWeatherDto first = day("2022-10-15", "2", "31", "35", "-2", "3", "11");
        AsosDailyWeatherDto missingRain = day("2022-10-16", null, "30", "31", "-1", "2", "9");
        AsosDailyWeatherDto second = day("2023-10-15", "0", "20", "25", "-12", null, "12");
        var result = calculator.calculate(List.of(
                new WeatherStatisticsCalculator.YearWeather(2022, List.of(first, missingRain)),
                new WeatherStatisticsCalculator.YearWeather(2023, List.of(second))));

        assertEquals(1, result.occurrenceYears());
        assertEquals(new BigDecimal("50.00"), result.occurrenceRate());
        assertEquals(2, result.validRainDays());
        assertEquals(1, result.rainDays());
        assertEquals(new BigDecimal("50.00"), result.rainDayRate());
        assertEquals(new BigDecimal("1.00"), result.averageRainfallMm());
        assertEquals(1, result.hotOccurrenceYears());
        assertEquals(1, result.coldOccurrenceYears());
        assertEquals(2, result.strongWindDays());
        assertEquals(2, result.strongWindOccurrenceYears());
        assertEquals(12, result.monthlyRainOccurrenceRates().size());
        assertEquals(new BigDecimal("50.00"), result.monthlyRainOccurrenceRates().get(9).occurrenceRate());
        assertNull(result.monthlyRainOccurrenceRates().get(0).occurrenceRate());
    }

    @Test
    void calculatesSameMonthDayAndClampsLeapDay() {
        assertEquals(LocalDate.of(2022, 2, 28), WeatherRiskAnalyzer.sameMonthDay(2022, LocalDate.of(2024, 2, 29)));
        assertEquals(LocalDate.of(2026, 10, 15), WeatherRiskAnalyzer.sameMonthDay(2026, LocalDate.of(2027, 10, 15)));
    }

    private AsosDailyWeatherDto day(String date, String rain, String avg, String max, String min, String wind, String maxWind) {
        return new AsosDailyWeatherDto(LocalDate.parse(date), decimal(rain), decimal(avg), decimal(max), decimal(min), decimal(wind), decimal(maxWind));
    }
    private BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }
}
