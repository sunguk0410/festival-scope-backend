package likelion.festivalscope.analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import likelion.festivalscope.common.entity.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@Entity
@Table(name = "regional_visitor_stat",
        uniqueConstraints = @UniqueConstraint(name = "uk_regional_visitor_stat_date_region",
                columnNames = {"base_ymd", "signgu_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionalVisitorStat extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "regional_visitor_stat_id")
    private Long regionalVisitorStatId;

    @Column(name = "base_ymd", nullable = false)
    private LocalDate baseYmd;

    @Column(name = "signgu_code", nullable = false, length = 20)
    private String signguCode;

    @Column(name = "signgu_name", nullable = false, length = 100)
    private String signguName;

    @Column(name = "sido_name", length = 100)
    private String sidoName;

    @Column(name = "visitor_count", nullable = false)
    private Long visitorCount;
}
