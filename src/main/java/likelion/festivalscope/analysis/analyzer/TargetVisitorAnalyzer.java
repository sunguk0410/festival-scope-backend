package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.plan.entity.*;
import likelion.festivalscope.festival.entity.*;
import likelion.festivalscope.festival.repository.FestivalHistoryRepository;
import likelion.festivalscope.festival.repository.FestivalRepository;
import likelion.festivalscope.festival.repository.FestivalThemeRepository;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TargetVisitorAnalyzer {
    private final FestivalRepository festivalRepository;
    private final FestivalThemeRepository festivalThemeRepository;
    private final FestivalHistoryRepository festivalHistoryRepository;

    public Result analyze(FestivalPlan plan, List<FestivalPlanTheme> planThemes) {
        if (plan.getTargetVisitorCount() == null || plan.getTargetVisitorCount() <= 0) {
            throw new AnalysisExecutionException("紐⑺몴 諛⑸Ц媛??섍? ?덉뼱??TARGET_VISITOR 遺꾩꽍???ㅽ뻾?????덉뒿?덈떎.");
        }

        Map<Long, FestivalHistory> latestHistoryByFestival = new LinkedHashMap<>();
        festivalHistoryRepository.findAllByVisitorCountIsNotNullOrderByYearDesc()
                .forEach(history -> latestHistoryByFestival.putIfAbsent(history.getFestival().getFestivalId(), history));

        List<Festival> festivals = festivalRepository.findAll().stream()
                .filter(festival -> latestHistoryByFestival.containsKey(festival.getFestivalId()))
                .toList();
        if (festivals.isEmpty()) {
            throw new AnalysisExecutionException("諛⑸Ц媛??섍? ?덈뒗 異뺤젣 ?대젰???놁뼱 TARGET_VISITOR 遺꾩꽍???ㅽ뻾?????놁뒿?덈떎.");
        }

        Map<Long, List<FestivalTheme>> festivalThemes = festivalThemeRepository
                .findAllByFestival_FestivalIdIn(festivals.stream().map(Festival::getFestivalId).toList())
                .stream()
                .collect(Collectors.groupingBy(theme -> theme.getFestival().getFestivalId()));

        List<Candidate> candidates = festivals.stream()
                .map(festival -> createCandidate(plan, planThemes, festival,
                        festivalThemes.getOrDefault(festival.getFestivalId(), List.of()),
                        latestHistoryByFestival.get(festival.getFestivalId())))
                .sorted(Comparator.comparing(Candidate::similarityScore).reversed()
                        .thenComparing(candidate -> candidate.history().getVisitorCount(), Comparator.reverseOrder()))
                .limit(5)
                .toList();

        if (candidates.isEmpty()) {
            throw new AnalysisExecutionException("?좎궗 異뺤젣 ?꾨낫瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        BigDecimal median = calculateMedian(candidates.stream()
                .map(candidate -> candidate.history().getVisitorCount())
                .sorted()
                .toList());
        BigDecimal target = BigDecimal.valueOf(plan.getTargetVisitorCount());
        BigDecimal ratio = target.divide(median, 4, RoundingMode.HALF_UP);

        return new Result(candidates, median, ratio, calculateScore(ratio));
    }

    private Candidate createCandidate(FestivalPlan plan, List<FestivalPlanTheme> planThemes,
                                      Festival festival, List<FestivalTheme> festivalThemes,
                                      FestivalHistory history) {
        BigDecimal themeSimilarity = percentage(themeMatchCount(planThemes, festivalThemes), planThemes.size());
        BigDecimal regionSimilarity = regionSimilarity(plan, festival);
        BigDecimal periodSimilarity = periodSimilarity(plan, history);
        BigDecimal score = themeSimilarity.multiply(new BigDecimal("0.6"))
                .add(regionSimilarity.multiply(new BigDecimal("0.2")))
                .add(periodSimilarity.multiply(new BigDecimal("0.2")))
                .setScale(2, RoundingMode.HALF_UP);
        return new Candidate(festival, history, score, themeSimilarity, regionSimilarity, periodSimilarity);
    }

    private long themeMatchCount(List<FestivalPlanTheme> planThemes, List<FestivalTheme> festivalThemes) {
        return planThemes.stream().filter(planTheme -> festivalThemes.stream().anyMatch(festivalTheme ->
                Objects.equals(planTheme.getThemeCode(), festivalTheme.getThemeCode())
                        || Objects.equals(planTheme.getThemeTag(), festivalTheme.getThemeTag()))).count();
    }

    private BigDecimal percentage(long matched, int total) {
        if (total == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(matched * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal regionSimilarity(FestivalPlan plan, Festival festival) {
        if (!Objects.equals(plan.getSido(), festival.getSido())) return BigDecimal.ZERO;
        if (plan.getSigungu() != null && Objects.equals(plan.getSigungu(), festival.getSigungu())) {
            return BigDecimal.valueOf(100).setScale(2);
        }
        return BigDecimal.valueOf(50).setScale(2);
    }

    private BigDecimal periodSimilarity(FestivalPlan plan, FestivalHistory history) {
        if (plan.getStartDate() == null || history.getStartDate() == null) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(plan.getStartDate().getMonthValue() == history.getStartDate().getMonthValue() ? 100 : 0)
                .setScale(2);
    }

    private BigDecimal calculateMedian(List<Long> sortedValues) {
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) return BigDecimal.valueOf(sortedValues.get(middle));
        return BigDecimal.valueOf(sortedValues.get(middle - 1))
                .add(BigDecimal.valueOf(sortedValues.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateScore(BigDecimal ratio) {
        if (ratio.compareTo(BigDecimal.ONE) <= 0) return BigDecimal.valueOf(100);
        if (ratio.compareTo(new BigDecimal("1.2")) <= 0) return BigDecimal.valueOf(85);
        if (ratio.compareTo(new BigDecimal("1.5")) <= 0) return BigDecimal.valueOf(70);
        if (ratio.compareTo(new BigDecimal("2.0")) <= 0) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf(30);
    }

    public record Result(List<Candidate> candidates, BigDecimal median, BigDecimal ratio, BigDecimal score) {}

    public record Candidate(Festival festival, FestivalHistory history, BigDecimal similarityScore,
                            BigDecimal themeSimilarity, BigDecimal regionSimilarity,
                            BigDecimal periodSimilarity) {}
}
