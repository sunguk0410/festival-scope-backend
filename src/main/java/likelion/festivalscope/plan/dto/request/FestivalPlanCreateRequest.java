package likelion.festivalscope.plan.dto.request;

import jakarta.validation.constraints.*;
import likelion.festivalscope.plan.entity.FestivalStatus;
import likelion.festivalscope.plan.entity.VenueType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FestivalPlanCreateRequest(
        @NotNull Long userId,
        @Size(max = 200) String planName,
        @NotBlank @Size(max = 200) String festivalName,
        @NotNull FestivalStatus festivalStatus,
        @Min(1) @Max(9999) Integer firstHeldYear,
        @NotBlank @Size(max = 50) String sido,
        @NotBlank @Size(max = 50) String sigungu,
        @NotBlank @Size(max = 200) String venueName,
        @NotBlank @Size(max = 255) String venueAddress,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        @DecimalMin("0") BigDecimal budget,
        @PositiveOrZero Long targetVisitorCount,
        VenueType venueType,
        @PositiveOrZero Integer capacity,
        List<@NotBlank @Size(max = 20) String> themeCodes,
        List<@NotBlank @Size(max = 200) String> programNames
) {}
