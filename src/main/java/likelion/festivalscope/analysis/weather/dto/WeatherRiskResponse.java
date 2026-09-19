package likelion.festivalscope.analysis.weather.dto;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.plan.entity.VenueType;
import likelion.festivalscope.analysis.dto.response.RecommendationResponse;
import likelion.festivalscope.analysis.dto.response.ResultInterpretation;

import java.math.BigDecimal;
import java.util.List;

public record WeatherRiskResponse(AnalysisItemType itemType, BigDecimal score, Station station,
                                  AnalysisPeriod analysisPeriod, Rain rain, Temperature temperature,
                                  Wind wind, FestivalCondition festivalCondition,
                                  List<RecommendationResponse> recommendations,
                                  ResultInterpretation resultInterpretation) {
    public record Station(String stationId, String stationName, BigDecimal distanceKm) {}
    public record AnalysisPeriod(int requestedYears, int actualYears, int startYear, int endYear, int totalDays) {}
    public record Rain(int occurrenceYears, BigDecimal occurrenceRate, int validDays, int rainDays,
                       BigDecimal rainDayRate, BigDecimal averageRainfallMm,
                       List<MonthlyRainOccurrence> monthlyRainOccurrenceRates) {}
    public record MonthlyRainOccurrence(int month, int validDays, int rainDays, BigDecimal occurrenceRate) {}
    public record Temperature(int validDays, BigDecimal averageTemperature, BigDecimal averageMaxTemperature,
                              BigDecimal averageMinTemperature, int hotOccurrenceYears, BigDecimal hotOccurrenceRate,
                              int coldOccurrenceYears, BigDecimal coldOccurrenceRate) {}
    public record Wind(int validDays, BigDecimal averageWindSpeed, BigDecimal maxWindSpeed,
                       int strongWindOccurrenceYears, BigDecimal strongWindOccurrenceRate,
                       int strongWindDays, BigDecimal strongWindDayRate) {}
    public record FestivalCondition(VenueType spaceType) {}
}
