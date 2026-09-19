package likelion.festivalscope.analysis.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import likelion.festivalscope.plan.entity.VenueType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@Entity
@Table(name = "festival_analysis_interpretation_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_interpretation_snapshot_item", columnNames = "festival_analysis_item_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisInterpretationSnapshot extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interpretation_snapshot_id")
    private Long interpretationSnapshotId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_analysis_item_id", nullable = false, unique = true)
    private FestivalAnalysisItem festivalAnalysisItem;

    @Column(nullable = false, length = 1000) private String status;
    @Enumerated(EnumType.STRING) @Column(name = "status_level", length = 20) private StatusLevel statusLevel;
    @Column(nullable = false, columnDefinition = "TEXT") private String summary;
    @Column(nullable = false, columnDefinition = "TEXT") private String detail;

    @Column(name = "comparison_median", precision = 15, scale = 2) private BigDecimal comparisonMedian;
    @Column(name = "gap_rate", precision = 10, scale = 2) private BigDecimal gapRate;
    @Column(name = "target_ratio", precision = 15, scale = 6) private BigDecimal targetRatio;

    @Column(name = "latest_growth_rate", precision = 10, scale = 2) private BigDecimal latestGrowthRate;
    @Column(name = "declining_keyword_rate", precision = 10, scale = 2) private BigDecimal decliningKeywordRate;
    @Column(name = "event_period_gap", precision = 10, scale = 2) private BigDecimal eventPeriodGap;

    @Column(name = "region_percentile", precision = 10, scale = 2) private BigDecimal regionPercentile;
    @Column(name = "month_percentile", precision = 10, scale = 2) private BigDecimal monthPercentile;
    @Column(name = "event_month_rank") private Integer eventMonthRank;
    @Column(name = "current_week_rank") private Integer currentWeekRank;

    @Column(name = "direct_overlap_count") private Integer directOverlapCount;
    @Column(name = "nearby_period_count") private Integer nearbyPeriodCount;
    @Column(name = "possible_conflict_count") private Integer possibleConflictCount;
    @Column(name = "historical_event_years") private Integer historicalEventYears;
    @Column(name = "history_years") private Integer historyYears;

    @Column(name = "rain_occurrence_rate", precision = 10, scale = 2) private BigDecimal rainOccurrenceRate;
    @Enumerated(EnumType.STRING) @Column(name = "temperature_type", length = 20) private TemperatureType temperatureType;
    @Column(name = "temperature_occurrence_rate", precision = 10, scale = 2) private BigDecimal temperatureOccurrenceRate;
    @Column(name = "wind_occurrence_rate", precision = 10, scale = 2) private BigDecimal windOccurrenceRate;
    @Enumerated(EnumType.STRING) @Column(name = "space_type", length = 20) private VenueType spaceType;
    @Column(name = "high_risk_count") private Integer highRiskCount;
    @Column(name = "moderate_risk_count") private Integer moderateRiskCount;

    @Column(name = "total_poi_within_5km") private Integer totalPoiWithin5km;
    @Column(name = "tourism_culture_within_3km") private Integer tourismCultureWithin3km;
    @Column(name = "food_shopping_within_3km") private Integer foodShoppingWithin3km;
    @Column(name = "accommodation_within_5km") private Integer accommodationWithin5km;
    @Column(name = "high_potential_count") private Integer highPotentialCount;
    @Column(name = "low_potential_count") private Integer lowPotentialCount;

    public enum TemperatureType { HOT, COLD, NEUTRAL }
}
