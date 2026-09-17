package likelion.festivalscope.plan.service;

import likelion.festivalscope.global.exception.ResourceNotFoundException;
import likelion.festivalscope.plan.dto.request.FestivalPlanCreateRequest;
import likelion.festivalscope.plan.dto.response.FestivalPlanCreateResponse;
import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.plan.entity.FestivalPlanProgram;
import likelion.festivalscope.plan.entity.FestivalPlanTheme;
import likelion.festivalscope.plan.repository.FestivalPlanProgramRepository;
import likelion.festivalscope.plan.repository.FestivalPlanRepository;
import likelion.festivalscope.plan.repository.FestivalPlanThemeRepository;
import likelion.festivalscope.user.entity.User;
import likelion.festivalscope.user.repository.UserRepository;
import likelion.festivalscope.festival.repository.FestivalThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FestivalPlanService {
    private final UserRepository userRepository;
    private final FestivalPlanRepository festivalPlanRepository;
    private final FestivalPlanThemeRepository festivalPlanThemeRepository;
    private final FestivalPlanProgramRepository festivalPlanProgramRepository;
    private final FestivalThemeRepository festivalThemeRepository;

    @Transactional
    public FestivalPlanCreateResponse create(FestivalPlanCreateRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long userId) || !userId.equals(request.userId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다: " + request.userId()));

        FestivalPlan plan = festivalPlanRepository.save(FestivalPlan.builder()
                .user(user)
                .planName(request.planName())
                .festivalName(request.festivalName())
                .sido(request.sido())
                .sigungu(request.sigungu())
                .venueName(request.venueName())
                .venueAddress(request.venueAddress())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .durationDays(calculateDurationDays(request))
                .budget(request.budget())
                .targetVisitorCount(request.targetVisitorCount())
                .venueType(request.venueType())
                .capacity(request.capacity())
                .festivalStatus(request.festivalStatus())
                .firstHeldYear(request.firstHeldYear())
                .build());

        List<String> themeCodes = request.themeCodes() == null ? List.of() : request.themeCodes();
        themeCodes.stream()
                .map(theme -> FestivalPlanTheme.builder()
                        .festivalPlan(plan)
                        .themeCode(theme)
                        .themeTag(festivalThemeRepository.findFirstByThemeCode(theme)
                                .map(likelion.festivalscope.festival.entity.FestivalTheme::getThemeTag)
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST)))
                        .build())
                .forEach(festivalPlanThemeRepository::save);

        List<String> programNames = request.programNames() == null ? List.of() : request.programNames();
        programNames.stream()
                .map(program -> FestivalPlanProgram.builder()
                        .festivalPlan(plan)
                        .programName(program)
                        .build())
                .forEach(festivalPlanProgramRepository::save);

        return new FestivalPlanCreateResponse(
                plan.getFestivalPlanId(),
                plan.getVenueName(),
                plan.getVenueAddress(),
                plan.getLatitude(),
                plan.getLongitude());
    }

    private Integer calculateDurationDays(FestivalPlanCreateRequest request) {
        if (request.startDate() == null || request.endDate() == null) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1);
    }
}
