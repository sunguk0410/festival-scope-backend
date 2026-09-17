package likelion.festivalscope.analysis.conflict;

import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.festival.entity.*;
import likelion.festivalscope.festival.repository.FestivalHistoryRepository;
import likelion.festivalscope.plan.entity.FestivalPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
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
        // 시군구는 DB 조회 단계에서 제한한다. 시도는 원천 데이터의 "경기"/"경기도"처럼
        // 기존 표기가 섞여 있어 조회 후 표준화하여 동일 지역 여부를 확인한다.
        List<FestivalHistory> all = historyRepository.findAllByFestival_SigunguAndYearBetween(plan.getSigungu(), historyStart, targetYear);
        List<Candidate> candidates = new ArrayList<>();
        for (FestivalHistory history : all) {
            if (!usable(history) || !sameRegion(plan, history.getFestival())) continue;
            if (isSelf(plan, history.getFestival())) continue;
            if (history.getYear() == targetYear) {
                ConflictType type = classifyConfirmed(plan, history);
                if (type != null) candidates.add(new Candidate(history, type, EventBasis.CONFIRMED, overlap(plan.getStartDate(), plan.getEndDate(), history.getStartDate(), history.getEndDate()), RegionRelation.SAME_REGION));
            } else if (history.getYear() >= historyStart && history.getYear() <= historyEnd) {
                LocalDate windowStart = sameMonthDay(plan.getStartDate(), history.getYear()).minusDays(HISTORICAL_DAYS);
                LocalDate windowEnd = sameMonthDay(plan.getEndDate(), history.getYear()).plusDays(HISTORICAL_DAYS);
                if (!history.getStartDate().isAfter(windowEnd) && !history.getEndDate().isBefore(windowStart)) {
                    candidates.add(new Candidate(history, ConflictType.HISTORICAL_SAME_PERIOD, EventBasis.HISTORICAL, 0, RegionRelation.SAME_REGION));
                }
            }
        }
        // 동일 축제가 여러 연도에 반복된 경우에는 가장 최근 이력만 근거로 남긴다.
        Map<Long, Candidate> latestByFestival = new LinkedHashMap<>();
        candidates.stream()
                .sorted(Comparator.comparingInt((Candidate c) -> c.history().getYear())
                        .thenComparingInt(c -> priority(c.conflictType())))
                .forEach(candidate -> latestByFestival.put(candidate.history().getFestival().getFestivalId(), candidate));
        return new Result(historyStart, historyEnd, new ArrayList<>(latestByFestival.values()));
    }

    private ConflictType classifyConfirmed(FestivalPlan plan, FestivalHistory h) {
        if (overlaps(plan.getStartDate(), plan.getEndDate(), h.getStartDate(), h.getEndDate())) return ConflictType.DIRECT_OVERLAP;
        LocalDate start = plan.getStartDate().minusDays(NEARBY_DAYS), end = plan.getEndDate().plusDays(NEARBY_DAYS);
        return !h.getStartDate().isAfter(end) && !h.getEndDate().isBefore(start) ? ConflictType.NEARBY_PERIOD : null;
    }
    private boolean isSelf(FestivalPlan p, Festival f) { return Objects.equals(normalize(p.getFestivalName()), normalize(f.getFestivalName())); }
    private boolean sameRegion(FestivalPlan p, Festival f) {
        return Objects.equals(normalizeSido(p.getSido()), normalizeSido(f.getSido()))
                && Objects.equals(normalize(p.getSigungu()), normalize(f.getSigungu()));
    }
    private boolean usable(FestivalHistory h) { return h.getStartDate() != null && h.getEndDate() != null && h.getFestival() != null; }
    private boolean overlaps(LocalDate a, LocalDate b, LocalDate c, LocalDate d) { return !c.isAfter(b) && !d.isBefore(a); }
    private int overlap(LocalDate a, LocalDate b, LocalDate c, LocalDate d) { if (!overlaps(a,b,c,d)) return 0; return (int)(Duration.between(c.isAfter(a)?c:a, d.isBefore(b)?d:b).toDays()+1); }
    private LocalDate sameMonthDay(LocalDate date, int year) { return MonthDay.of(date.getMonthValue(), Math.min(date.getDayOfMonth(), YearMonth.of(year, date.getMonthValue()).lengthOfMonth())).atYear(year); }
    private String normalize(String value) { return value == null ? "" : value.replace(" ", "").trim(); }
    private String normalizeSido(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "경기" -> "경기도";
            case "강원" -> "강원도";
            case "충북" -> "충청북도";
            case "충남" -> "충청남도";
            case "전북" -> "전라북도";
            case "전남" -> "전라남도";
            case "경북" -> "경상북도";
            case "경남" -> "경상남도";
            case "제주" -> "제주특별자치도";
            default -> normalized;
        };
    }
    private int priority(ConflictType type) {
        return switch (type) {
            case DIRECT_OVERLAP -> 0;
            case NEARBY_PERIOD -> 1;
            case HISTORICAL_SAME_PERIOD -> 2;
        };
    }

    public record Candidate(FestivalHistory history, ConflictType conflictType, EventBasis eventBasis, Integer overlapDays, RegionRelation regionRelation) {}
    public record Result(int historyStartYear, int historyEndYear, List<Candidate> candidates) {
        public long count(ConflictType type) { return candidates.stream().filter(c -> c.conflictType() == type).count(); }
    }
}
