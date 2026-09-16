package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;

import lombok.*;
import java.math.BigDecimal;

@Getter @Builder @Entity @Table(name = "festival_analysis_poi")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisPoi extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_analysis_poi_id") private Long festivalAnalysisPoiId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_item_id", nullable = false) private FestivalAnalysisItem festivalAnalysisItem;
    @Column(name = "content_id", length = 100) private String contentId;
    @Column(name = "poi_name", nullable = false, length = 200) private String poiName;
    @Enumerated(EnumType.STRING) @Column(name = "poi_type", nullable = false, length = 30) private PoiType poiType;
    @Column(name = "distance_m") private Integer distanceM;
    @Column(precision = 10, scale = 7) private BigDecimal latitude;
    @Column(precision = 10, scale = 7) private BigDecimal longitude;
    @Enumerated(EnumType.STRING) @Column(name = "linkage_type", length = 30) private LinkageType linkageType;
}
