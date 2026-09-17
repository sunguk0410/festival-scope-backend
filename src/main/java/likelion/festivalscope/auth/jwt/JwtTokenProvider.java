package likelion.festivalscope.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey key; private final long accessExpiration; private final long refreshExpiration;
    public JwtTokenProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.access-token-expiration:3600000}") long accessExpiration, @Value("${jwt.refresh-token-expiration:1209600000}") long refreshExpiration) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.accessExpiration=accessExpiration; this.refreshExpiration=refreshExpiration;
    }
    public String createAccessToken(Long userId, String email) { return create(userId,email,accessExpiration,"ACCESS"); }
    public String createRefreshToken(Long userId) { return create(userId,null,refreshExpiration,"REFRESH"); }
    private String create(Long userId,String email,long expiration,String type){ Instant now=Instant.now(); JwtBuilder b=Jwts.builder().subject(String.valueOf(userId)).claim("type",type).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expiration))).signWith(key); if(email!=null)b.claim("email",email); return b.compact(); }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public boolean isValid(String token) { try { parse(token); return true; } catch (JwtException|IllegalArgumentException e) { return false; } }
    public boolean isExpired(String token) { try { parse(token); return false; } catch (ExpiredJwtException e) { return true; } catch (JwtException|IllegalArgumentException e) { return false; } }
    public Long getUserId(String token) { return Long.valueOf(parse(token).getSubject()); }
    public String getEmail(String token) { return parse(token).get("email", String.class); }
    public long getAccessExpirationSeconds() { return accessExpiration / 1000; }
    public long getRefreshExpirationSeconds() { return refreshExpiration / 1000; }
}
