package likelion.festivalscope.auth.dto.response;
public record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, MeResponse user) {}
