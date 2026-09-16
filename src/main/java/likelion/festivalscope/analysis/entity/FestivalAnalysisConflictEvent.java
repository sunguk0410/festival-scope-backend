package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.festival.entity.Festival;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import likelion.festivalscope.analysis.entity.EventBasis;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder @Entity @Table(name = "festival_analysis_conflict_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisConflictEvent extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "conflict_event_id") private Long conflictEventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_item_id", nullable = false) private FestivalAnalysisItem festivalAnalysisItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "festival_id") private Festival festival;
    @Column(name = "event_name", nullable = false, length = 200) private String eventName;
    @Column(length = 50) private String sido;
    @Column(length = 50) private String sigungu;
    @Column(name = "event_year") private Integer eventYear;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(name = "distance_km", precision = 8, scale = 2) private BigDecimal distanceKm;
    @Column(name = "overlap_days") private Integer overlapDays;
    @Column(name = "same_theme") private Boolean sameTheme;
    @Enumerated(EnumType.STRING) @Column(name = "event_basis", nullable = false, length = 20) private EventBasis eventBasis;
}
