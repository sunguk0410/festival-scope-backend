package likelion.festivalscope.festival.entity;

import likelion.festivalscope.common.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Builder @Entity @Table(name = "festival_theme", uniqueConstraints = @UniqueConstraint(name = "uk_festival_theme", columnNames = {"festival_id", "theme_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalTheme extends CreatedAtEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "festival_theme_id") private Long festivalThemeId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "festival_id", nullable = false) private Festival festival;
    @Column(name = "theme_code", nullable = false, length = 20) private String themeCode;
    @Column(name = "theme_tag", nullable = false, length = 100) private String themeTag;
}
