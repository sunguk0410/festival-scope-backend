package likelion.festivalscope.festival.entity;

import likelion.festivalscope.common.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Builder @Entity @Table(name = "festival", uniqueConstraints = @UniqueConstraint(name = "uk_festival_code", columnNames = "festival_code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Festival extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_id") private Long festivalId;
    @Column(name = "festival_code", nullable = false, length = 20) private String festivalCode;
    @Column(name = "festival_name", nullable = false, length = 200) private String festivalName;
    @Column(nullable = false, length = 50) private String sido;
    @Column(length = 50) private String sigungu;
    @Column(name = "festival_type", length = 50) private String festivalType;
    @Column(name = "first_year") private Integer firstYear;
    @Column(length = 255) private String venue;
}
