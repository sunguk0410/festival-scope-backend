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
    @Column(name = "tourism_culture_within_3km_count") private Integer tourismCultureWithin3kmCount;
    @Column(name = "tourism_culture_between_3_and_5km_count") private Integer tourismCultureBetween3And5kmCount;
    @Column(name = "tourism_culture_within_5km_count") private Integer tourismCultureWithin5kmCount;
    @Column(name = "food_shopping_within_3km_count") private Integer foodShoppingWithin3kmCount;
    @Column(name = "food_shopping_between_3_and_5km_count") private Integer foodShoppingBetween3And5kmCount;
    @Column(name = "food_shopping_within_5km_count") private Integer foodShoppingWithin5kmCount;
    @Column(name = "accommodation_within_3km_count") private Integer accommodationWithin3kmCount;
    @Column(name = "accommodation_between_3_and_5km_count") private Integer accommodationBetween3And5kmCount;
    @Column(name = "accommodation_within_5km_count") private Integer accommodationWithin5kmCount;
    @Column(name = "resource_demand_base_ym", length = 6) private String resourceDemandBaseYm;
    @Column(name = "resource_demand_code", length = 10) private String resourceDemandCode;
    @Column(name = "resource_demand_name", length = 100) private String resourceDemandName;
    @Column(name = "resource_demand_value", precision = 15, scale = 4) private java.math.BigDecimal resourceDemandValue;
    @Column(name = "consumption_intensity_base_ym", length = 6) private String consumptionIntensityBaseYm;
    @Column(name = "consumption_intensity_code", length = 10) private String consumptionIntensityCode;
    @Column(name = "consumption_intensity_name", length = 100) private String consumptionIntensityName;
    @Column(name = "consumption_intensity_value", precision = 15, scale = 4) private java.math.BigDecimal consumptionIntensityValue;
    @Column(name = "stay_intensity_base_ym", length = 6) private String stayIntensityBaseYm;
    @Column(name = "stay_intensity_code", length = 10) private String stayIntensityCode;
    @Column(name = "stay_intensity_name", length = 100) private String stayIntensityName;
    @Column(name = "stay_intensity_value", precision = 15, scale = 4) private java.math.BigDecimal stayIntensityValue;
    @Column(name = "tourism_linkage_summary", length = 1000) private String tourismLinkageSummary;
    @Column(name = "consumption_linkage_summary", length = 1000) private String consumptionLinkageSummary;
    @Column(name = "stay_linkage_summary", length = 1000) private String stayLinkageSummary;
}
