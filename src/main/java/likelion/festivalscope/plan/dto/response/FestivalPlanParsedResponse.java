package likelion.festivalscope.plan.dto.response;

import likelion.festivalscope.plan.entity.FestivalStatus;
import likelion.festivalscope.plan.entity.VenueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FestivalPlanParsedResponse(
        String planName,
        String festivalName,
        FestivalStatus festivalStatus,
        Integer firstHeldYear,
        String sido,
        String sigungu,
        String venueName,
        String venueAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate startDate,
        LocalDate endDate,
        Long targetVisitorCount,
        VenueType venueType,
        Integer capacity,
        List<ParsedThemeDto> themes,
        List<String> programNames
) {}
