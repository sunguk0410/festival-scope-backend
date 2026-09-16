package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.festival.entity.FestivalHistory;

import likelion.festivalscope.festival.entity.Festival;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Builder @Entity @Table(name = "festival_analysis_similar")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisSimilar extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_analysis_similar_id") private Long festivalAnalysisSimilarId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_item_id", nullable = false) private FestivalAnalysisItem festivalAnalysisItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "festival_id") private Festival festival;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "festival_history_id") private FestivalHistory festivalHistory;
    @Column(name = "festival_name", nullable = false, length = 200) private String festivalName;
    private Integer year;
    @Column(name = "visitor_count") private Long visitorCount;
    @Column(precision = 15, scale = 2) private BigDecimal budget;
    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 2) private BigDecimal similarityScore;
    @Column(name = "theme_similarity", precision = 5, scale = 2) private BigDecimal themeSimilarity;
    @Column(name = "region_similarity", precision = 5, scale = 2) private BigDecimal regionSimilarity;
    @Column(name = "period_similarity", precision = 5, scale = 2) private BigDecimal periodSimilarity;
    @Column(name = "rank_order", nullable = false) private Integer rankOrder;
}
