package likelion.festivalscope.plan.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Builder @Entity @Table(name = "festival_plan_program")
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalPlanProgram extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_plan_program_id") private Long festivalPlanProgramId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_plan_id", nullable = false) private FestivalPlan festivalPlan;
    @Column(name = "program_name", nullable = false, length = 200) private String programName;
}
