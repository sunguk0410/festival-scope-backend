package likelion.festivalscope.analysis.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import lombok.*;
import java.time.LocalDate;

@Getter @Builder @Entity @Table(name = "festival_analysis_conflict",
        uniqueConstraints = @UniqueConstraint(name = "uk_analysis_conflict_item", columnNames = "festival_analysis_item_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisConflict extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conflict_analysis_id") private Long conflictAnalysisId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_analysis_item_id", nullable = false, unique = true)
    private FestivalAnalysisItem festivalAnalysisItem;
    @Column(name = "target_start_date") private LocalDate targetStartDate;
    @Column(name = "target_end_date") private LocalDate targetEndDate;
    @Column(name = "history_start_year") private Integer historyStartYear;
    @Column(name = "history_end_year") private Integer historyEndYear;
    @Column(name = "direct_overlap_count") private Integer directOverlapCount;
    @Column(name = "nearby_period_count") private Integer nearbyPeriodCount;
    @Column(name = "historical_same_period_count") private Integer historicalSamePeriodCount;
    @Column(name = "same_region_count") private Integer sameRegionCount;
    @Column(name = "neighbor_region_count") private Integer neighborRegionCount;
}
