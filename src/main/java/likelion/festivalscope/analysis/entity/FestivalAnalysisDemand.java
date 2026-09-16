package likelion.festivalscope.analysis.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "festival_analysis_demand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisDemand extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "festival_analysis_demand_id")
    private Long festivalAnalysisDemandId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_analysis_item_id", nullable = false)
    private FestivalAnalysisItem festivalAnalysisItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "demand_type", nullable = false, length = 20)
    private DemandType demandType;

    @Column(name = "region_code", length = 30)
    private String regionCode;

    @Column(nullable = false, length = 50)
    private String sido;

    @Column(length = 50)
    private String sigungu;

    @Column(name = "stat_year", nullable = false)
    private Integer statYear;

    @Column(name = "stat_month")
    private Integer statMonth;

    @Column(name = "stat_day")
    private Integer statDay;

    @Column(name = "visitor_count", nullable = false)
    private Long visitorCount;
}