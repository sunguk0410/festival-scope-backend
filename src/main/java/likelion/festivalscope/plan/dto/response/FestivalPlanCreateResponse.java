package likelion.festivalscope.plan.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record FestivalPlanCreateResponse(
        @Schema(description = "생성된 FestivalPlan ID", example = "1") Long planId,
        @Schema(description = "선택한 행사장 명칭", example = "자라섬 남도") String venueName,
        @Schema(description = "선택한 행사장 도로명/지번 주소", example = "경기도 가평군 가평읍 자라섬로 60") String venueAddress,
        @Schema(description = "행사장 위도", example = "37.8181234") BigDecimal latitude,
        @Schema(description = "행사장 경도", example = "127.5212345") BigDecimal longitude
) {}