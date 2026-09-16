package likelion.festivalscope.external.weather.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AsosDailyWeatherDto(LocalDate date, BigDecimal rainfallMm, BigDecimal averageTemperature,
                                  BigDecimal maximumTemperature, BigDecimal minimumTemperature,
                                  BigDecimal averageWindSpeed, BigDecimal maximumWindSpeed) {
}
