package likelion.festivalscope.auth.repository;
import likelion.festivalscope.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser_UserId(Long userId);
    void deleteByUser_UserId(Long userId);
}
