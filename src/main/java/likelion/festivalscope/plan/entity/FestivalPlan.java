package likelion.festivalscope.plan.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.BaseTimeEntity;
import likelion.festivalscope.user.entity.User;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@Entity
@Table(name = "festival_plan")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalPlan extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "festival_plan_id")
    private Long festivalPlanId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_name", length = 200)
    private String planName;

    @Column(name = "festival_name", nullable = false, length = 200)
    private String festivalName;
    @Enumerated(EnumType.STRING) @Column(name = "festival_status", length = 20) private FestivalStatus festivalStatus;
    @Column(name = "first_held_year") private Integer firstHeldYear;

    @Column(nullable = false, length = 50)
    private String sido;

    @Column(length = 50)
    private String sigungu;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "venue_address", length = 255)
    private String venueAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "target_visitor_count")
    private Long targetVisitorCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_type", length = 20)
    private VenueType venueType;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;
}
