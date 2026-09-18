package likelion.festivalscope.analysis.weather;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.external.weather.KmaAsosClient;
import likelion.festivalscope.external.weather.dto.AsosDailyWeatherDto;
import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.weather.service.WeatherStationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class WeatherRiskAnalyzer {
    private final KmaAsosClient client;
    private final WeatherStationService stationService;
    private final int requestedYears;
    private final WeatherStatisticsCalculator calculator;

    public WeatherRiskAnalyzer(KmaAsosClient client, WeatherStationService stationService,
                               @Value("${kma.asos.analysis-years:${WEATHER_ANALYSIS_YEARS:5}}") int requestedYears,
                               @Value("${kma.asos.rain-threshold:${WEATHER_RAIN_THRESHOLD:0}}") BigDecimal rainThreshold,
                               @Value("${kma.asos.hot-day-threshold:${HOT_DAY_THRESHOLD:33}}") BigDecimal hotThreshold,
                               @Value("${kma.asos.cold-day-threshold:${COLD_DAY_THRESHOLD:-10}}") BigDecimal coldThreshold,
                               @Value("${kma.asos.strong-wind-threshold:${STRONG_WIND_THRESHOLD:10}}") BigDecimal strongWindThreshold) {
        this.client = client; this.stationService = stationService; this.requestedYears = requestedYears;
        this.calculator = new WeatherStatisticsCalculator(rainThreshold, hotThreshold, coldThreshold, strongWindThreshold);
    }

    public WeatherRiskResponse analyze(FestivalPlan plan) {
        if (plan.getStartDate() == null || plan.getEndDate() == null || plan.getLatitude() == null || plan.getLongitude() == null)
            throw new IllegalArgumentException("Weather risk analysis requires festival dates and venue coordinates");
        WeatherStationService.Selection selected = stationService.findNearest(plan.getLatitude(), plan.getLongitude());
        // 개최 예정 연도보다 과거인 연도만 우선 후보로 삼되, 현재 연도도
        // 동일 기간의 종료일까지 자료가 공개됐다면 분석에 포함할 수 있다.
        int latestYear = Math.min(plan.getStartDate().getYear() - 1, LocalDate.now().getYear());
        LocalDate latestAvailableDate = LocalDate.now().minusDays(1);
        log.info("WEATHER_RISK analysis started: stationId={}, stationName={}, distanceKm={}, latestAllowedYear={}, requestedYears={}",
                selected.station().getStationId(), selected.station().getStationName(), selected.distanceKm(), latestYear, requestedYears);
        List<WeatherStatisticsCalculator.YearWeather> years = new java.util.ArrayList<>();
        for (int year = latestYear; year >= 1 && years.size() < requestedYears; year--) {
            LocalDate from = sameMonthDay(year, plan.getStartDate());
            LocalDate to = sameMonthDay(year, plan.getEndDate());
            if (to.isAfter(latestAvailableDate)) {
                log.info("WEATHER_RISK year skipped because requested period is not fully available: year={}, from={}, to={}, latestAvailableDate={}",
                        year, from, to, latestAvailableDate);
                continue;
            }
            log.info("WEATHER_RISK year request: year={}, from={}, to={}", year, from, to);
            List<AsosDailyWeatherDto> days = client.getDailyWeather(selected.station().getStationId(), from, to);
            if (!days.isEmpty()) years.add(new WeatherStatisticsCalculator.YearWeather(year, days));
        }
        years.sort(Comparator.comparingInt(WeatherStatisticsCalculator.YearWeather::year));
        WeatherStatisticsCalculator.Result result = calculator.calculate(years);
        int totalDays = years.stream().flatMap(y -> y.days().stream()).map(AsosDailyWeatherDto::date).distinct().toList().size();
        int startYear = years.isEmpty() ? latestYear : years.get(0).year();
        int endYear = years.isEmpty() ? latestYear : years.get(years.size() - 1).year();
        log.info("WEATHER_RISK analysis completed: actualYears={}, years={}, totalDays={}",
                years.size(), years.stream().map(WeatherStatisticsCalculator.YearWeather::year).toList(), totalDays);
        return new WeatherRiskResponse(AnalysisItemType.WEATHER_RISK, null,
                new WeatherRiskResponse.Station(selected.station().getStationId(), selected.station().getStationName(), BigDecimal.valueOf(selected.distanceKm()).setScale(2, java.math.RoundingMode.HALF_UP)),
                new WeatherRiskResponse.AnalysisPeriod(requestedYears, years.size(), startYear, endYear, totalDays),
                new WeatherRiskResponse.Rain(result.occurrenceYears(), result.occurrenceRate(), result.validRainDays(), result.rainDays(), result.rainDayRate(), result.averageRainfallMm()),
                new WeatherRiskResponse.Temperature(result.validTemperatureDays(), result.averageTemperature(), result.averageMaxTemperature(), result.averageMinTemperature(), result.hotOccurrenceYears(), result.hotOccurrenceRate(), result.coldOccurrenceYears(), result.coldOccurrenceRate()),
                new WeatherRiskResponse.Wind(result.validWindDays(), result.averageWindSpeed(), result.maxWindSpeed(), result.strongWindOccurrenceYears(), result.strongWindOccurrenceRate(), result.strongWindDays(), result.strongWindDayRate()),
                new WeatherRiskResponse.FestivalCondition(plan.getVenueType()),
                java.util.List.of(), null);
    }

    static LocalDate sameMonthDay(int year, LocalDate date) {
        return YearMonth.of(year, date.getMonth()).atDay(Math.min(date.getDayOfMonth(), YearMonth.of(year, date.getMonth()).lengthOfMonth()));
    }
}
