package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import lombok.*;
import java.math.BigDecimal;

@Getter @Builder @Entity @Table(name = "festival_analysis_item", uniqueConstraints = @UniqueConstraint(name = "uk_festival_analysis_item", columnNames = {"festival_analysis_id", "item_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisItem extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_analysis_item_id") private Long festivalAnalysisItemId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_id", nullable = false) private FestivalAnalysis festivalAnalysis;
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false, length = 30) private AnalysisItemType itemType;
    @Column(precision = 5, scale = 2) private BigDecimal score;
}
