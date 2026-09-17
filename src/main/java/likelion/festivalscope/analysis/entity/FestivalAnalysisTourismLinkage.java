package likelion.festivalscope.analysis.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import lombok.*;

@Getter @Builder @Entity @Table(name = "festival_analysis_tourism_linkage",
        uniqueConstraints = @UniqueConstraint(name = "uk_tourism_linkage_item", columnNames = "festival_analysis_item_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisTourismLinkage extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tourism_linkage_analysis_id") private Long tourismLinkageAnalysisId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_analysis_item_id", nullable = false, unique = true)
    private FestivalAnalysisItem festivalAnalysisItem;
    @Column(name = "total_candidate_poi_count", nullable = false) private Integer totalCandidatePoiCount;
    @Column(name = "tourism_culture_count", nullable = false) private Integer tourismCultureCount;
    @Column(name = "food_shopping_count", nullable = false) private Integer foodShoppingCount;
    @Column(name = "accommodation_count", nullable = false) private Integer accommodationCount;
    @Column(name = "tourism_linkage_summary", length = 1000) private String tourismLinkageSummary;
    @Column(name = "consumption_linkage_summary", length = 1000) private String consumptionLinkageSummary;
    @Column(name = "stay_linkage_summary", length = 1000) private String stayLinkageSummary;
}
