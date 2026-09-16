package likelion.festivalscope.analysis.weather;

import likelion.festivalscope.external.weather.dto.AsosDailyWeatherDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

public class WeatherStatisticsCalculator {
    public static final BigDecimal DEFAULT_RAIN_THRESHOLD = BigDecimal.ZERO;
    public static final BigDecimal DEFAULT_HOT_DAY_THRESHOLD = BigDecimal.valueOf(33);
    public static final BigDecimal DEFAULT_COLD_DAY_THRESHOLD = BigDecimal.valueOf(-10);
    public static final BigDecimal DEFAULT_STRONG_WIND_THRESHOLD = BigDecimal.valueOf(10);

    private final BigDecimal rainThreshold;
    private final BigDecimal hotThreshold;
    private final BigDecimal coldThreshold;
    private final BigDecimal strongWindThreshold;

    public WeatherStatisticsCalculator(BigDecimal rainThreshold, BigDecimal hotThreshold,
                                       BigDecimal coldThreshold, BigDecimal strongWindThreshold) {
        this.rainThreshold = rainThreshold; this.hotThreshold = hotThreshold;
        this.coldThreshold = coldThreshold; this.strongWindThreshold = strongWindThreshold;
    }

    public Result calculate(List<YearWeather> years) {
        List<AsosDailyWeatherDto> all = years.stream().flatMap(y -> y.days().stream()).toList();
        List<BigDecimal> rain = all.stream().map(AsosDailyWeatherDto::rainfallMm).filter(v -> v != null).toList();
        List<BigDecimal> temp = all.stream().map(AsosDailyWeatherDto::averageTemperature).filter(v -> v != null).toList();
        List<BigDecimal> maxTemp = all.stream().map(AsosDailyWeatherDto::maximumTemperature).filter(v -> v != null).toList();
        List<BigDecimal> minTemp = all.stream().map(AsosDailyWeatherDto::minimumTemperature).filter(v -> v != null).toList();
        List<BigDecimal> avgWind = all.stream().map(AsosDailyWeatherDto::averageWindSpeed).filter(v -> v != null).toList();
        List<BigDecimal> maxWind = all.stream().map(AsosDailyWeatherDto::maximumWindSpeed).filter(v -> v != null).toList();
        int rainYears = (int) years.stream().filter(y -> y.days().stream().anyMatch(d -> d.rainfallMm() != null && d.rainfallMm().compareTo(rainThreshold) > 0)).count();
        int hotYears = (int) years.stream().filter(y -> y.days().stream().anyMatch(d -> d.maximumTemperature() != null && d.maximumTemperature().compareTo(hotThreshold) >= 0)).count();
        int coldYears = (int) years.stream().filter(y -> y.days().stream().anyMatch(d -> d.minimumTemperature() != null && d.minimumTemperature().compareTo(coldThreshold) <= 0)).count();
        List<AsosDailyWeatherDto> windDays = all.stream().filter(d -> d.maximumWindSpeed() != null && d.maximumWindSpeed().compareTo(strongWindThreshold) >= 0).toList();
        int windYears = (int) years.stream().filter(y -> y.days().stream().anyMatch(d -> d.maximumWindSpeed() != null && d.maximumWindSpeed().compareTo(strongWindThreshold) >= 0)).count();
        return new Result(rainYears, rate(rainYears, years.size()), rain.size(), rainDays(rain), rate(rainDays(rain), rain.size()), average(rain),
                temp.size(), average(temp), average(maxTemp), average(minTemp), hotYears, rate(hotYears, years.size()), coldYears, rate(coldYears, years.size()),
                avgWind.size(), average(avgWind), maxWind.stream().max(Comparator.naturalOrder()).orElse(null), windYears, rate(windYears, years.size()), windDays.size(), rate(windDays.size(), maxWind.size()));
    }

    private int rainDays(List<BigDecimal> values) { return (int) values.stream().filter(v -> v.compareTo(rainThreshold) > 0).count(); }
    private BigDecimal average(List<BigDecimal> values) { return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP); }
    private BigDecimal rate(int numerator, int denominator) { return denominator == 0 ? null : BigDecimal.valueOf(numerator * 100D / denominator).setScale(2, RoundingMode.HALF_UP); }

    public record YearWeather(int year, List<AsosDailyWeatherDto> days) {}
    public record Result(int occurrenceYears, BigDecimal occurrenceRate, int validRainDays, int rainDays, BigDecimal rainDayRate, BigDecimal averageRainfallMm,
                         int validTemperatureDays, BigDecimal averageTemperature, BigDecimal averageMaxTemperature, BigDecimal averageMinTemperature,
                         int hotOccurrenceYears, BigDecimal hotOccurrenceRate, int coldOccurrenceYears, BigDecimal coldOccurrenceRate,
                         int validWindDays, BigDecimal averageWindSpeed, BigDecimal maxWindSpeed, int strongWindOccurrenceYears,
                         BigDecimal strongWindOccurrenceRate, int strongWindDays, BigDecimal strongWindDayRate) {}
}
