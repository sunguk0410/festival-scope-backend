package likelion.festivalscope.plan.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import likelion.festivalscope.plan.entity.VenueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record FestivalPlanCreateRequest(
        @NotNull @Schema(description = "등록하는 사용자의 ID", example = "1") Long userId,
        @Size(max = 200) @Schema(description = "기획안 이름", example = "영월 가을별빛 야행축제 기획안") String planName,
        @NotBlank @Size(max = 200) @Schema(description = "축제명", example = "영월 가을별빛 야행축제") String festivalName,
        @NotBlank @Size(max = 50) @Schema(description = "시도", example = "강원특별자치도") String sido,
        @Size(max = 50) @Schema(description = "시군구", example = "영월군") String sigungu,
        @Size(max = 200) @Schema(description = "선택한 행사장 명칭", example = "자라섬 남도") String venueName,
        @Size(max = 255) @Schema(description = "선택한 행사장 도로명/지번 주소", example = "경기도 가평군 가평읍 자라섬로 60") String venueAddress,
        @DecimalMin("-90.0") @DecimalMax("90.0") @Schema(description = "행사장 위도", example = "37.8181234") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") @Schema(description = "행사장 경도", example = "127.5212345") BigDecimal longitude,
        @Schema(description = "축제 시작일", example = "2026-10-10") LocalDate startDate,
        @Schema(description = "축제 종료일", example = "2026-10-12") LocalDate endDate,
        @DecimalMin("0") @Schema(description = "축제 예산", example = "500000000") BigDecimal budget,
        @PositiveOrZero @Schema(description = "목표 방문객 수", example = "50000") Long targetVisitorCount,
        @Schema(description = "행사장 유형", example = "OUTDOOR") VenueType venueType,
        @Schema(description = "운영 시작 시간", example = "18:00") LocalTime operationStartTime,
        @Schema(description = "운영 종료 시간", example = "23:00") LocalTime operationEndTime,
        @PositiveOrZero @Schema(description = "수용 가능 인원", example = "10000") Integer capacity,
        @Schema(description = "우천 시 대체 계획 보유 여부", example = "false") Boolean rainPlanAvailable,
        @Valid @Schema(description = "기획안 테마 목록") List<ThemeRequest> themes,
        @Valid @Schema(description = "기획안 프로그램 목록") List<ProgramRequest> programs
) {
    public record ThemeRequest(
            @Size(max = 20) @Schema(description = "테마 코드", example = "CA03") String themeCode,
            @NotBlank @Size(max = 100) @Schema(description = "테마 태그", example = "빛·미디어아트") String themeTag
    ) {}

    public record ProgramRequest(
            @NotBlank @Size(max = 200) @Schema(description = "프로그램명", example = "야간 미디어아트 전시") String programName,
            @Size(max = 50) @Schema(description = "프로그램 유형", example = "EXHIBITION") String programType,
            @Size(max = 20) @Schema(description = "프로그램 공간 유형", example = "OUTDOOR") String spaceType,
            @Schema(description = "프로그램 설명", example = "문화유산 주변 야간 미디어아트 전시") String description
    ) {}
}