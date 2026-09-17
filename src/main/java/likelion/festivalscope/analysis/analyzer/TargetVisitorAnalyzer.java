package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.festival.entity.*;
import likelion.festivalscope.festival.repository.*;
import likelion.festivalscope.plan.entity.*;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.common.util.RegionNameNormalizer;
import likelion.festivalscope.common.util.FestivalNameNormalizer;
import likelion.festivalscope.analysis.entity.TargetVisitorComparisonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.*;
import java.util.*;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor @Slf4j
public class TargetVisitorAnalyzer {
    private static final double FESTIVAL_NAME_SIMILARITY_THRESHOLD = 0.85;
    private static final double AMBIGUOUS_SIMILARITY_MARGIN = 0.02;
    private static final BigDecimal THEME_WEIGHT = new BigDecimal("0.50");
    private static final BigDecimal REGION_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal PERIOD_WEIGHT = new BigDecimal("0.25");
    @Value("${analysis.target-visitor.similarity-threshold:${TARGET_VISITOR_SIMILARITY_THRESHOLD:60.00}}")
    private BigDecimal similarityThreshold;
    private final FestivalRepository festivalRepository;
    private final FestivalThemeRepository festivalThemeRepository;
    private final FestivalHistoryRepository festivalHistoryRepository;

    public Result analyze(FestivalPlan plan, List<FestivalPlanTheme> planThemes) {
        if (plan.getTargetVisitorCount() == null || plan.getTargetVisitorCount() <= 0)
            throw new AnalysisExecutionException("TARGET_VISITOR target visitor count is required");
        List<Festival> festivals = festivalRepository.findAll();
        List<Festival> regionalFestivals = festivalRepository.findAllBySidoAndSigungu(plan.getSido(), plan.getSigungu());
        if (regionalFestivals.isEmpty()) {
            regionalFestivals = festivalRepository.findAllBySigungu(plan.getSigungu()).stream()
                    .filter(festival -> RegionNameNormalizer.sameSido(plan.getSido(), festival.getSido()))
                    .toList();
        }
        Long targetFestivalId = plan.getFestivalStatus() == FestivalStatus.EXISTING
                ? findTargetFestivalId(plan, regionalFestivals) : null;
        Map<Long, List<FestivalTheme>> themes = festivalThemeRepository
                .findAllByFestival_FestivalIdIn(festivals.stream().map(Festival::getFestivalId).toList())
                .stream().collect(Collectors.groupingBy(t -> t.getFestival().getFestivalId()));
        List<FestivalHistory> histories = festivalHistoryRepository.findAll().stream()
                .filter(h -> h.getStartDate() != null && !sameTargetHistory(plan, targetFestivalId, h)).toList();
        List<Candidate> passedHistories = histories.stream()
                .map(h -> candidate(plan, targetFestivalId, planThemes, h,
                        h.getFestival() == null ? List.of() : themes.getOrDefault(h.getFestival().getFestivalId(), List.of())))
                .filter(c -> c.sameFestival() || c.similarityScore().compareTo(similarityThreshold) >= 0)
                .toList();
        List<Candidate> candidates = deduplicate(passedHistories);
        List<Long> visitors = candidates.stream().map(c -> c.history().getVisitorCount()).filter(Objects::nonNull).sorted().toList();
        BigDecimal average = visitors.isEmpty() ? null : BigDecimal.valueOf(visitors.stream().mapToLong(Long::longValue).sum())
                .divide(BigDecimal.valueOf(visitors.size()), 2, RoundingMode.HALF_UP);
        BigDecimal median = median(visitors);
        BigDecimal gap = median == null || median.signum() == 0 ? null : BigDecimal.valueOf(plan.getTargetVisitorCount())
                .subtract(median).divide(median, 4, RoundingMode.HALF_UP);
        log.info("TARGET_VISITOR candidates: totalHistories={}, thresholdPassedHistories={}, deduplicatedFestivals={}, visitorDataCount={}, average={}, median={}, targetVisitor={}, gapRate={}",
                histories.size(), passedHistories.size(), candidates.size(), visitors.size(), average, median, plan.getTargetVisitorCount(), gap);
        return new Result(candidates, passedHistories.size(), average, median,
                visitors.isEmpty() ? null : visitors.get(0), visitors.isEmpty() ? null : visitors.get(visitors.size() - 1), gap, similarityThreshold);
    }

    private List<Candidate> deduplicate(List<Candidate> candidates) {
        Comparator<Candidate> representative = Comparator
                .comparing((Candidate c) -> c.history().getVisitorCount() != null)
                .reversed().thenComparing(Candidate::similarityScore, Comparator.reverseOrder())
                .thenComparing(c -> c.history().getYear(), Comparator.reverseOrder());
        List<Candidate> sameFestivalHistories = candidates.stream()
                .filter(Candidate::sameFestival)
                .sorted(Comparator.comparing((Candidate c) -> c.history().getYear(), Comparator.reverseOrder())
                        .thenComparing(Candidate::similarityScore, Comparator.reverseOrder()))
                .toList();
        Map<String, Candidate> grouped = new HashMap<>();
        for (Candidate candidate : candidates.stream().filter(candidate -> !candidate.sameFestival()).toList()) {
            String key = candidate.festival() != null && candidate.festival().getFestivalId() != null
                    ? "F-" + candidate.festival().getFestivalId()
                    : "H-" + candidate.history().getFestivalHistoryId();
            grouped.merge(key, candidate, (left, right) -> representative.compare(left, right) <= 0 ? left : right);
        }
        List<Candidate> similarFestivals = grouped.values().stream().sorted(Comparator.comparing(Candidate::similarityScore, Comparator.reverseOrder())
                .thenComparing(c -> c.history().getYear(), Comparator.reverseOrder())
                .thenComparing(c -> c.festival() == null ? Long.MAX_VALUE : c.festival().getFestivalId(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return java.util.stream.Stream.concat(sameFestivalHistories.stream(), similarFestivals.stream()).toList();
    }

    private Candidate candidate(FestivalPlan plan, Long targetFestivalId, List<FestivalPlanTheme> planThemes,
                                FestivalHistory history, List<FestivalTheme> festivalThemes) {
        BigDecimal theme = themeSimilarity(planThemes, festivalThemes);
        BigDecimal region = regionSimilarity(plan, history.getFestival());
        BigDecimal period = periodSimilarity(plan, history);
        BigDecimal score = theme.multiply(THEME_WEIGHT).add(region.multiply(REGION_WEIGHT)).add(period.multiply(PERIOD_WEIGHT)).setScale(2, RoundingMode.HALF_UP);
        boolean sameFestival = targetFestivalId != null
                && history.getFestival() != null
                && targetFestivalId.equals(history.getFestival().getFestivalId());
        return new Candidate(history.getFestival(), history, score, theme, region, period, sameFestival);
    }

    private Long findTargetFestivalId(FestivalPlan plan, List<Festival> festivals) {
        List<Festival> regionalCandidates = festivals;
        List<Festival> exactMatches = regionalCandidates.stream()
                .filter(festival -> FestivalNameNormalizer.same(plan.getFestivalName(), festival.getFestivalName()))
                .toList();
        if (exactMatches.size() == 1) return exactMatches.get(0).getFestivalId();
        if (exactMatches.size() > 1) {
            List<Festival> firstYearMatches = firstYearMatches(plan, exactMatches);
            if (firstYearMatches.size() == 1) return firstYearMatches.get(0).getFestivalId();
            log.warn("TARGET_VISITOR exact festival match is ambiguous: planName={}, candidateIds={}",
                    plan.getFestivalName(), exactMatches.stream().map(Festival::getFestivalId).toList());
            return null;
        }

        List<FestivalMatch> similarMatches = regionalCandidates.stream()
                .map(festival -> new FestivalMatch(festival, FestivalNameNormalizer.similarity(
                        plan.getFestivalName(), festival.getFestivalName())))
                .filter(match -> match.similarity() >= FESTIVAL_NAME_SIMILARITY_THRESHOLD)
                .sorted(Comparator.comparingDouble(FestivalMatch::similarity).reversed()
                        .thenComparing(match -> firstYearMatches(plan, List.of(match.festival())).size() == 1 ? 0 : 1))
                .toList();
        if (similarMatches.isEmpty()) {
            log.warn("TARGET_VISITOR existing festival match not found: planName={}, sido={}, sigungu={}",
                    plan.getFestivalName(), plan.getSido(), plan.getSigungu());
            return null;
        }
        FestivalMatch best = similarMatches.get(0);
        List<FestivalMatch> tied = similarMatches.stream()
                .filter(match -> best.similarity() - match.similarity() <= AMBIGUOUS_SIMILARITY_MARGIN)
                .toList();
        List<FestivalMatch> tiedFirstYearMatches = tied.stream()
                .filter(match -> plan.getFirstHeldYear() != null
                        && plan.getFirstHeldYear().equals(match.festival().getFirstYear()))
                .toList();
        if (tiedFirstYearMatches.size() == 1) return tiedFirstYearMatches.get(0).festival().getFestivalId();
        if (tied.size() > 1) {
            log.warn("TARGET_VISITOR similar festival match is ambiguous: planName={}, candidateIds={}",
                    plan.getFestivalName(), tied.stream().map(match -> match.festival().getFestivalId()).toList());
            return null;
        }
        return best.festival().getFestivalId();
    }

    private List<Festival> firstYearMatches(FestivalPlan plan, List<Festival> candidates) {
        if (plan.getFirstHeldYear() == null) return List.of();
        return candidates.stream().filter(festival -> plan.getFirstHeldYear().equals(festival.getFirstYear())).toList();
    }

    private record FestivalMatch(Festival festival, double similarity) {}

    private BigDecimal themeSimilarity(List<FestivalPlanTheme> plan, List<FestivalTheme> candidate) {
        Set<String> a = plan.stream().map(this::themeKey).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> b = candidate.stream().map(this::themeKey).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> union = new HashSet<>(a); union.addAll(b);
        if (union.isEmpty()) return BigDecimal.ZERO.setScale(2);
        Set<String> intersection = new HashSet<>(a); intersection.retainAll(b);
        return BigDecimal.valueOf(intersection.size() * 100.0 / union.size()).setScale(2, RoundingMode.HALF_UP);
    }
    private String themeKey(FestivalPlanTheme t) { return t.getThemeCode() != null && !t.getThemeCode().isBlank() ? "code:" + t.getThemeCode() : key(t.getThemeTag()); }
    private String themeKey(FestivalTheme t) { return t.getThemeCode() != null && !t.getThemeCode().isBlank() ? "code:" + t.getThemeCode() : key(t.getThemeTag()); }
    private String key(String value) { return value == null ? null : value.trim().toLowerCase(Locale.ROOT); }
    private BigDecimal regionSimilarity(FestivalPlan p, Festival f) {
        if (f == null || !RegionNameNormalizer.sameSido(p.getSido(), f.getSido())) return BigDecimal.ZERO.setScale(2);
        return RegionNameNormalizer.sameSigungu(p.getSigungu(), f.getSigungu())
                ? BigDecimal.valueOf(100) : BigDecimal.valueOf(70);
    }
    private BigDecimal periodSimilarity(FestivalPlan p, FestivalHistory h) {
        if (p.getStartDate() == null) return BigDecimal.ZERO.setScale(2);
        int d = Math.abs(p.getStartDate().getMonthValue() - h.getStartDate().getMonthValue());
        d = Math.min(d, 12 - d);
        return BigDecimal.valueOf(d == 0 ? 100 : d == 1 ? 70 : d == 2 ? 40 : 0).setScale(2);
    }
    private boolean sameTargetHistory(FestivalPlan p, Long targetFestivalId, FestivalHistory h) {
        return h.getFestival() != null && p.getStartDate() != null && h.getYear() == p.getStartDate().getYear()
                && ((targetFestivalId != null && targetFestivalId.equals(h.getFestival().getFestivalId()))
                    || (targetFestivalId == null
                        && FestivalNameNormalizer.same(p.getFestivalName(), h.getFestival().getFestivalName())
                        && RegionNameNormalizer.sameSido(p.getSido(), h.getFestival().getSido())
                        && RegionNameNormalizer.sameSigungu(p.getSigungu(), h.getFestival().getSigungu())));
    }
    private boolean same(String a, String b) { return normalize(a).equals(normalize(b)); }
    private String normalize(String value) { return value == null ? "" : value.replace(" ", "").trim(); }
    private BigDecimal median(List<Long> values) { if (values.isEmpty()) return null; int m = values.size() / 2; return values.size() % 2 == 1 ? BigDecimal.valueOf(values.get(m)) : BigDecimal.valueOf(values.get(m - 1)).add(BigDecimal.valueOf(values.get(m))).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP); }
    public record Result(List<Candidate> candidates, int thresholdPassedHistoryCount, BigDecimal visitorAverage, BigDecimal visitorMedian, Long visitorMin, Long visitorMax, BigDecimal gapRate, BigDecimal similarityThreshold) {}
    public record Candidate(Festival festival, FestivalHistory history, BigDecimal similarityScore, BigDecimal themeSimilarity, BigDecimal regionSimilarity, BigDecimal periodSimilarity, boolean sameFestival) {
        public TargetVisitorComparisonType comparisonType() {
            return sameFestival ? TargetVisitorComparisonType.SAME_FESTIVAL : TargetVisitorComparisonType.SIMILAR_FESTIVAL;
        }
    }
}
