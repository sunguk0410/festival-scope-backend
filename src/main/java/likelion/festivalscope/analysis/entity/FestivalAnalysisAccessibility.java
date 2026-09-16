package likelion.festivalscope.analysis.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@Entity
@Table(name = "festival_analysis_accessibility", uniqueConstraints = @UniqueConstraint(name = "uk_analysis_accessibility_item", columnNames = "festival_analysis_item_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisAccessibility extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accessibility_id")
    private Long accessibilityId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_analysis_item_id", nullable = false)
    private FestivalAnalysisItem festivalAnalysisItem;

    @Column(name = "nearest_bus_stop_name", length = 200)
    private String nearestBusStopName;

    @Column(name = "nearest_bus_stop_distance_m")
    private Integer nearestBusStopDistanceM;

    @Column(name = "bus_stop_count_500m")
    private Integer busStopCount500m;

    @Column(name = "bus_stop_count_1km")
    private Integer busStopCount1km;

    @Column(name = "bus_route_count")
    private Integer busRouteCount;

    @Column(name = "rail_available")
    private Boolean railAvailable;

    @Column(name = "nearest_station_name", length = 200)
    private String nearestStationName;

    @Column(name = "nearest_station_distance_m")
    private Integer nearestStationDistanceM;

    @Column(name = "bus_stop_count")
    private Integer busStopCount;

    @Column(name = "parking_count")
    private Integer parkingCount;

    @Column(name = "parking_capacity")
    private Integer parkingCapacity;

    @Column(name = "accessibility_score", precision = 5, scale = 2)
    private BigDecimal accessibilityScore;
}