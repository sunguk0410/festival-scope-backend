package likelion.festivalscope.auth.dto.response;
public record TokenRefreshResponse(String accessToken, String tokenType, long expiresIn) {}
