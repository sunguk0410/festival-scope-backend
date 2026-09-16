package likelion.festivalscope.user.entity;

import likelion.festivalscope.common.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Builder @Entity @Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "user_id") private Long userId;
    @Column(nullable = false, length = 255) private String email;
    @Column(nullable = false, length = 255) private String password;
    @Column(length = 100) private String name;
}
