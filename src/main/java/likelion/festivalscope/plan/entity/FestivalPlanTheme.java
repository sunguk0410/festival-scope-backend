package likelion.festivalscope.plan.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Builder @Entity @Table(name = "festival_plan_theme", uniqueConstraints = @UniqueConstraint(name = "uk_festival_plan_theme", columnNames = {"festival_plan_id", "theme_tag"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalPlanTheme extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_plan_theme_id") private Long festivalPlanThemeId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_plan_id", nullable = false) private FestivalPlan festivalPlan;
    @Column(name = "theme_code", length = 20) private String themeCode;
    @Column(name = "theme_tag", nullable = false, length = 100) private String themeTag;
    @Column(precision = 5, scale = 4) private BigDecimal confidence;
}
