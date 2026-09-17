package likelion.festivalscope.auth.entity;

import jakarta.persistence.*;
import likelion.festivalscope.common.entity.BaseTimeEntity;
import likelion.festivalscope.user.entity.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @Entity @Table(name="refresh_tokens", uniqueConstraints={
        @UniqueConstraint(name="uk_refresh_token_user", columnNames="user_id"),
        @UniqueConstraint(name="uk_refresh_token_value", columnNames="token")})
@NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor(access=AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="refresh_token_id") private Long refreshTokenId;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false, length=1000) private String token;
    @Column(name="expires_at", nullable=false) private LocalDateTime expiresAt;
    public void update(String token, LocalDateTime expiresAt) { this.token=token; this.expiresAt=expiresAt; }
}
