package likelion.festivalscope.analysis.entity;

import likelion.festivalscope.plan.entity.FestivalPlan;

import likelion.festivalscope.festival.entity.Festival;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import likelion.festivalscope.analysis.entity.AnalysisStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder @Entity @Table(name = "festival_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysis extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_analysis_id") private Long festivalAnalysisId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_plan_id", nullable = false) private FestivalPlan festivalPlan;
    @Column(name = "analysis_version", nullable = false, length = 30) private String analysisVersion;
    @Column(name = "total_score", precision = 5, scale = 2) private BigDecimal totalScore;
    @Enumerated(EnumType.STRING) @Column(name = "analysis_status", nullable = false, length = 20) private AnalysisStatus analysisStatus;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
}
