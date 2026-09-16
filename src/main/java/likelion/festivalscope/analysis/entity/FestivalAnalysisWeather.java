package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder @Entity @Table(name = "festival_analysis_weather")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisWeather extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "weather_id") private Long weatherId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_item_id", nullable = false) private FestivalAnalysisItem festivalAnalysisItem;
    @Column(name = "observation_year", nullable = false) private Integer observationYear;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(name = "avg_temp", precision = 5, scale = 2) private BigDecimal avgTemp;
    @Column(name = "max_temp", precision = 5, scale = 2) private BigDecimal maxTemp;
    @Column(name = "min_temp", precision = 5, scale = 2) private BigDecimal minTemp;
    @Column(name = "rainfall_mm", precision = 8, scale = 2) private BigDecimal rainfallMm;
    @Column(name = "rain_days") private Integer rainDays;
    @Column(name = "max_wind_speed", precision = 6, scale = 2) private BigDecimal maxWindSpeed;
    @Column(name = "heatwave_days") private Integer heatwaveDays;
    @Column(name = "coldwave_days") private Integer coldwaveDays;
}
