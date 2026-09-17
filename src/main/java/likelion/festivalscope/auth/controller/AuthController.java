package likelion.festivalscope.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import likelion.festivalscope.auth.dto.request.*;
import likelion.festivalscope.auth.dto.response.*;
import likelion.festivalscope.auth.service.AuthService;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 계정을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않음"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "로그인", description = "로그인 성공 시 Access Token과 Refresh Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Access Token 재발급", description = "Refresh Token을 검증하여 새로운 Access Token을 발급합니다. Refresh Token은 요청 본문으로 전달합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access Token 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 없거나 만료 또는 유효하지 않음")
    })
    @PostMapping("/reissue")
    public TokenRefreshResponse reissue(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.reissue(request);
    }

    @Operation(summary = "로그아웃", description = "전달받은 Refresh Token을 서버에서 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token을 찾을 수 없음")
    })
    @PostMapping("/logout")
    public void logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request);
    }

    @Operation(summary = "현재 사용자 조회", description = "Bearer Access Token으로 인증된 사용자 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 조회 성공"),
            @ApiResponse(responseCode = "401", description = "Access Token이 없거나 유효하지 않음")
    })
    @GetMapping("/me")
    public MeResponse me(@Parameter(hidden = true) Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return authService.me(userId);
    }
}
