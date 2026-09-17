package likelion.festivalscope.analysis.entity;
import jakarta.persistence.*;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import lombok.*;
import java.math.BigDecimal;
@Getter @Builder @Entity @Table(name="festival_analysis_target_visitor", uniqueConstraints=@UniqueConstraint(name="uk_target_visitor_item", columnNames="festival_analysis_item_id"))
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PROTECTED)
public class FestivalAnalysisTargetVisitor extends CreatedAtEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="target_visitor_analysis_id") private Long targetVisitorAnalysisId;
 @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="festival_analysis_item_id", nullable=false, unique=true) private FestivalAnalysisItem festivalAnalysisItem;
 @Column(name="target_visitor_count") private Long targetVisitorCount; @Column(name="similar_festival_count") private Integer similarFestivalCount; @Column(name="visitor_data_count") private Integer visitorDataCount;
 @Column(name="visitor_average", precision=15, scale=2) private BigDecimal visitorAverage; @Column(name="visitor_median", precision=15, scale=2) private BigDecimal visitorMedian; @Column(name="visitor_min") private Long visitorMin; @Column(name="visitor_max") private Long visitorMax;
 @Column(name="gap_rate", precision=10, scale=4) private BigDecimal gapRate; @Column(name="similarity_threshold", precision=5, scale=2) private BigDecimal similarityThreshold;
}
