package likelion.festivalscope.festival.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder @Entity @Table(name = "festival_history", uniqueConstraints = @UniqueConstraint(name = "uk_festival_history", columnNames = {"festival_id", "year"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalHistory extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_history_id") private Long festivalHistoryId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_id", nullable = false) private Festival festival;
    @Column(nullable = false) private Integer year;
    @Column(name = "festival_name_raw", nullable = false, length = 200) private String festivalNameRaw;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(precision = 15, scale = 2) private BigDecimal budget;
    @Column(name = "visitor_count") private Long visitorCount;
    @Column(name = "venue_raw", length = 255) private String venueRaw;
}
