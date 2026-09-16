package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import likelion.festivalscope.analysis.entity.PeriodType;
import lombok.*;
import java.math.BigDecimal;

@Getter @Builder @Entity @Table(name = "festival_analysis_trend_keyword")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisTrendKeyword extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "trend_keyword_id") private Long trendKeywordId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_analysis_item_id", nullable = false) private FestivalAnalysisItem festivalAnalysisItem;
    @Column(nullable = false, length = 100) private String keyword;
    @Enumerated(EnumType.STRING) @Column(name = "period_type", nullable = false, length = 20) private PeriodType periodType;
    @Column(name = "period_year", nullable = false) private Integer periodYear;
    @Column(name = "period_month") private Integer periodMonth;
    @Column(name = "interest_value", nullable = false, precision = 10, scale = 2) private BigDecimal interestValue;
}
