package likelion.festivalscope.analysis.conflict;

import likelion.festivalscope.analysis.entity.ConflictType;
import likelion.festivalscope.analysis.entity.EventBasis;
import likelion.festivalscope.analysis.entity.RegionRelation;
import likelion.festivalscope.festival.entity.Festival;
import likelion.festivalscope.festival.entity.FestivalHistory;
import likelion.festivalscope.festival.repository.FestivalHistoryRepository;
import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.common.util.RegionNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleConflictAnalyzer {
    private static final int HISTORY_YEARS = 5;
    private static final int NEARBY_DAYS = 3;
    private static final int HISTORICAL_DAYS = 7;

    private final FestivalHistoryRepository historyRepository;

    public Result analyze(FestivalPlan plan) {
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            throw new IllegalArgumentException("FestivalPlan startDate/endDate are required for CONFLICT_RISK");
        }

        int targetYear = plan.getStartDate().getYear();
        int historyStart = targetYear - HISTORY_YEARS;
        int historyEnd = targetYear - 1;
        List<FestivalHistory> histories = historyRepository
                .findAllByFestival_SigunguAndYearBetween(plan.getSigungu(), historyStart, targetYear);
        List<Candidate> candidates = new ArrayList<>();

        for (FestivalHistory history : histories) {
            if (!usable(history) || !sameRegion(plan, history.getFestival()) || isSelf(plan, history.getFestival())) {
                continue;
            }

            if (history.getYear() == targetYear) {
                ConflictType type = classifyConfirmed(plan, history);
                if (type != null) {
                    candidates.add(new Candidate(history, type, EventBasis.CONFIRMED,
                            overlap(plan.getStartDate(), plan.getEndDate(), history.getStartDate(), history.getEndDate()),
                            RegionRelation.SAME_REGION));
                }
                continue;
            }

            if (history.getYear() >= historyStart && history.getYear() <= historyEnd) {
                LocalDate windowStart = sameMonthDay(plan.getStartDate(), history.getYear()).minusDays(HISTORICAL_DAYS);
                LocalDate windowEnd = sameMonthDay(plan.getEndDate(), history.getYear()).plusDays(HISTORICAL_DAYS);
                if (!history.getStartDate().isAfter(windowEnd) && !history.getEndDate().isBefore(windowStart)) {
                    candidates.add(new Candidate(history, ConflictType.HISTORICAL_SAME_PERIOD,
                            EventBasis.HISTORICAL, 0, RegionRelation.SAME_REGION));
                }
            }
        }

        Map<Long, Candidate> latestByFestival = new LinkedHashMap<>();
        candidates.stream()
                .sorted(Comparator.comparingInt((Candidate candidate) -> candidate.history().getYear())
                        .thenComparingInt(candidate -> priority(candidate.conflictType())))
                .forEach(candidate -> latestByFestival.put(candidate.history().getFestival().getFestivalId(), candidate));
        return new Result(historyStart, historyEnd, new ArrayList<>(latestByFestival.values()));
    }

    private ConflictType classifyConfirmed(FestivalPlan plan, FestivalHistory history) {
        if (overlaps(plan.getStartDate(), plan.getEndDate(), history.getStartDate(), history.getEndDate())) {
            return ConflictType.DIRECT_OVERLAP;
        }
        LocalDate start = plan.getStartDate().minusDays(NEARBY_DAYS);
        LocalDate end = plan.getEndDate().plusDays(NEARBY_DAYS);
        return !history.getStartDate().isAfter(end) && !history.getEndDate().isBefore(start)
                ? ConflictType.NEARBY_PERIOD : null;
    }

    private boolean isSelf(FestivalPlan plan, Festival festival) {
        return Objects.equals(normalize(plan.getFestivalName()), normalize(festival.getFestivalName()));
    }

    private boolean sameRegion(FestivalPlan plan, Festival festival) {
        return RegionNameNormalizer.sameSido(plan.getSido(), festival.getSido())
                && RegionNameNormalizer.sameSigungu(plan.getSigungu(), festival.getSigungu());
    }

    private boolean usable(FestivalHistory history) {
        return history.getStartDate() != null && history.getEndDate() != null && history.getFestival() != null;
    }

    private boolean overlaps(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd) {
        return !secondStart.isAfter(firstEnd) && !secondEnd.isBefore(firstStart);
    }

    private int overlap(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd) {
        if (!overlaps(firstStart, firstEnd, secondStart, secondEnd)) return 0;
        LocalDate start = secondStart.isAfter(firstStart) ? secondStart : firstStart;
        LocalDate end = secondEnd.isBefore(firstEnd) ? secondEnd : firstEnd;
        return (int) (Duration.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).toDays());
    }

    private LocalDate sameMonthDay(LocalDate date, int year) {
        return MonthDay.of(date.getMonthValue(), Math.min(date.getDayOfMonth(),
                YearMonth.of(year, date.getMonthValue()).lengthOfMonth())).atYear(year);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").trim();
    }

    private String normalizeSido(String value) {
        return normalize(value)
                .replace("특별자치도", "")
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("자치도", "")
                .replace("도", "")
                .trim();
    }

    private int priority(ConflictType type) {
        return switch (type) {
            case DIRECT_OVERLAP -> 0;
            case NEARBY_PERIOD -> 1;
            case HISTORICAL_SAME_PERIOD -> 2;
        };
    }

    public record Candidate(FestivalHistory history, ConflictType conflictType, EventBasis eventBasis,
                            Integer overlapDays, RegionRelation regionRelation) {}

    public record Result(int historyStartYear, int historyEndYear, List<Candidate> candidates) {
        public long count(ConflictType type) {
            return candidates.stream().filter(candidate -> candidate.conflictType() == type).count();
        }
    }
}
