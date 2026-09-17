package likelion.festivalscope.region.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "region_code", uniqueConstraints = @UniqueConstraint(
        name = "uk_region_code_sido_sigungu", columnNames = {"sido_name", "sigungu_name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_code_id")
    private Long regionCodeId;

    @Column(name = "sido_name", nullable = false, length = 50)
    private String sidoName;

    @Column(name = "sigungu_name", nullable = false, length = 50)
    private String sigunguName;

    @Column(name = "area_cd", nullable = false, length = 10)
    private String areaCd;

    @Column(name = "sigungu_cd", nullable = false, length = 10)
    private String sigunguCd;
}
