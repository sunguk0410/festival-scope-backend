package likelion.festivalscope.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@Entity
@Table(name = "weather_station", indexes = @Index(name = "idx_weather_station_coordinates", columnList = "latitude, longitude"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherStation {
    @Id
    @Column(name = "station_id", length = 10)
    private String stationId;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
}
