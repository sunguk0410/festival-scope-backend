package likelion.festivalscope.station.entity;

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
@Table(name = "station", indexes = @Index(name = "idx_station_coordinates", columnList = "latitude, longitude"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Station {
    @Id
    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "line_name", length = 200)
    private String lineName;
}
