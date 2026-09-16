package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import lombok.*;

@Getter @Builder @Entity @Table(name = "festival_analysis_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisRecommendation extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "recommendation_id") private Long recommendationId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_id", nullable = false) private FestivalAnalysis festivalAnalysis;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "festival_analysis_item_id") private FestivalAnalysisItem festivalAnalysisItem;
    @Column(name = "recommendation_type", nullable = false, length = 30) private String recommendationType;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RecommendationPriority priority;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
}
