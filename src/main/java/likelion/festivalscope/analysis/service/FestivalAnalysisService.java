package likelion.festivalscope.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.analysis.analyzer.TargetVisitorAnalyzer;
import likelion.festivalscope.analysis.analyzer.TrendAnalysisResult;
import likelion.festivalscope.analysis.analyzer.TrendFitAnalyzer;
import likelion.festivalscope.analysis.analyzer.DemandFitAnalyzer;
import likelion.festivalscope.analysis.dto.response.DemandFitResponse;
import likelion.festivalscope.analysis.weather.WeatherRiskAnalyzer;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.analysis.conflict.ScheduleConflictAnalyzer;
import likelion.festivalscope.analysis.dto.response.ConflictRiskResponse;
import likelion.festivalscope.analysis.dto.response.FestivalAnalysisResponse;
import likelion.festivalscope.analysis.dto.response.TargetVisitorResponse;
import likelion.festivalscope.analysis.dto.response.TrendFitResponse;
import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.plan.entity.*;
import likelion.festivalscope.festival.entity.*;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.AnalysisStatus;
import likelion.festivalscope.analysis.repository.*;
import likelion.festivalscope.plan.repository.*;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalAnalysisService {
    private final FestivalPlanRepository festivalPlanRepository;
    private final FestivalPlanThemeRepository festivalPlanThemeRepository;
    private final FestivalAnalysisRepository festivalAnalysisRepository;
    private final FestivalAnalysisItemRepository festivalAnalysisItemRepository;
    private final FestivalAnalysisSimilarRepository festivalAnalysisSimilarRepository;
    private final TargetVisitorAnalyzer targetVisitorAnalyzer;
    private final TrendFitAnalyzer trendFitAnalyzer;
    private final FestivalAnalysisTrendKeywordRepository festivalAnalysisTrendKeywordRepository;
    private final FestivalAnalysisDemandRepository festivalAnalysisDemandRepository;
    private final FestivalAnalysisAccessibilityRepository festivalAnalysisAccessibilityRepository;
    private final DemandFitAnalyzer demandFitAnalyzer;
    private final WeatherRiskAnalyzer weatherRiskAnalyzer;
    private final FestivalAnalysisWeatherRiskSnapshotRepository weatherRiskSnapshotRepository;
    private final FestivalAnalysisConflictRepository festivalAnalysisConflictRepository;
    private final FestivalAnalysisConflictEventRepository festivalAnalysisConflictEventRepository;
    private final ScheduleConflictAnalyzer scheduleConflictAnalyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(noRollbackFor = AnalysisExecutionException.class)
    public Long execute(Long planId) {
        FestivalPlan plan = festivalPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("湲고쉷?덉쓣 李얠쓣 ???놁뒿?덈떎: " + planId));
        LocalDateTime startedAt = LocalDateTime.now();
        FestivalAnalysis analysis = festivalAnalysisRepository.save(FestivalAnalysis.builder()
                .festivalPlan(plan)
                .analysisVersion("v1.0")
                .analysisStatus(AnalysisStatus.PROCESSING)
                .startedAt(startedAt)
                .build());

        try {
            List<FestivalAnalysisItem> items = Arrays.stream(AnalysisItemType.values())
                    .map(itemType -> FestivalAnalysisItem.builder()
                            .festivalAnalysis(analysis)
                            .itemType(itemType)
                            .build())
                    .toList();
            festivalAnalysisItemRepository.saveAll(items);

            FestivalAnalysisItem targetVisitorItem = items.stream()
                    .filter(item -> item.getItemType() == AnalysisItemType.TARGET_VISITOR)
                    .findFirst()
                    .orElseThrow();
            TargetVisitorAnalyzer.Result result = targetVisitorAnalyzer.analyze(
                    plan, festivalPlanThemeRepository.findAllByFestivalPlan_FestivalPlanId(planId));

            for (int index = 0; index < result.candidates().size(); index++) {
                TargetVisitorAnalyzer.Candidate candidate = result.candidates().get(index);
                festivalAnalysisSimilarRepository.save(FestivalAnalysisSimilar.builder()
                        .festivalAnalysisItem(targetVisitorItem)
                        .festival(candidate.festival())
                        .festivalHistory(candidate.history())
                        .festivalName(candidate.festival().getFestivalName())
                        .year(candidate.history().getYear())
                        .visitorCount(candidate.history().getVisitorCount())
                        .budget(candidate.history().getBudget())
                        .similarityScore(candidate.similarityScore())
                        .themeSimilarity(candidate.themeSimilarity())
                        .regionSimilarity(candidate.regionSimilarity())
                        .periodSimilarity(candidate.periodSimilarity())
                        .rankOrder(index + 1)
                        .build());
            }

            FestivalAnalysisItem trendFitItem = items.stream()
                    .filter(item -> item.getItemType() == AnalysisItemType.TREND_FIT)
                    .findFirst()
                    .orElseThrow();
            TrendAnalysisResult trendResult = trendFitAnalyzer.analyze(plan);
            saveTrendKeywords(trendFitItem, trendResult);
            FestivalAnalysisItem demandFitItem = items.stream().filter(item -> item.getItemType() == AnalysisItemType.DEMAND_FIT).findFirst().orElseThrow();
            DemandFitAnalyzer.Result demandResult = demandFitAnalyzer.analyze(plan);
            saveDemandSnapshots(demandFitItem, plan, demandResult);

            FestivalAnalysisItem conflictItem = items.stream().filter(item -> item.getItemType() == AnalysisItemType.CONFLICT_RISK).findFirst().orElseThrow();
            saveConflictSnapshot(conflictItem, plan, scheduleConflictAnalyzer.analyze(plan));

            FestivalAnalysisItem weatherRiskItem = items.stream()
                    .filter(item -> item.getItemType() == AnalysisItemType.WEATHER_RISK)
                    .findFirst()
                    .orElseThrow();
            saveWeatherRiskSnapshot(weatherRiskItem, weatherRiskAnalyzer.analyze(plan));

            replaceItemScore(targetVisitorItem, result.score());
            replaceAnalysisStatus(analysis, result.score(), AnalysisStatus.COMPLETED, LocalDateTime.now());
            return analysis.getFestivalAnalysisId();
        } catch (Exception exception) {
            replaceAnalysisStatus(analysis, null, AnalysisStatus.FAILED, LocalDateTime.now());
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            if (exception instanceof AnalysisExecutionException analysisException) {
                throw analysisException;
            }
            throw new AnalysisExecutionException("異뺤젣 遺꾩꽍 ?ㅽ뻾???ㅽ뙣?덉뒿?덈떎.", exception);
        }
    }

    @Transactional(readOnly = true)
    public FestivalAnalysisResponse getAnalysis(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        List<FestivalAnalysisResponse.ItemResponse> items = festivalAnalysisItemRepository
                .findAllByFestivalAnalysis_FestivalAnalysisIdOrderByFestivalAnalysisItemIdAsc(analysisId)
                .stream()
                .map(item -> new FestivalAnalysisResponse.ItemResponse(
                        item.getFestivalAnalysisItemId(), item.getItemType(), item.getScore()))
                .toList();
        return new FestivalAnalysisResponse(
                analysis.getFestivalAnalysisId(),
                analysis.getFestivalPlan().getFestivalPlanId(),
                analysis.getFestivalPlan().getFestivalName(),
                analysis.getTotalScore(),
                analysis.getAnalysisStatus(),
                analysis.getCreatedAt(),
                items);
    }

    @Transactional(readOnly = true)
    public TargetVisitorResponse getTargetVisitor(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository
                .findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.TARGET_VISITOR)
                .orElseThrow(() -> new ResourceNotFoundException("TARGET_VISITOR ??ぉ??李얠쓣 ???놁뒿?덈떎: " + analysisId));
        List<FestivalAnalysisSimilar> similarFestivals = festivalAnalysisSimilarRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByRankOrderAsc(item.getFestivalAnalysisItemId());
        BigDecimal median = calculateMedian(similarFestivals.stream()
                .map(FestivalAnalysisSimilar::getVisitorCount)
                .sorted()
                .toList());
        BigDecimal target = BigDecimal.valueOf(analysis.getFestivalPlan().getTargetVisitorCount());
        BigDecimal ratio = target.divide(median, 4, RoundingMode.HALF_UP);
        List<TargetVisitorResponse.SimilarFestivalResponse> similarResponses = similarFestivals.stream()
                .map(similar -> new TargetVisitorResponse.SimilarFestivalResponse(
                        similar.getFestivalName(), similar.getYear(), similar.getVisitorCount(),
                        similar.getSimilarityScore(), similar.getRankOrder()))
                .toList();
        return new TargetVisitorResponse(item.getItemType(), item.getScore(),
                analysis.getFestivalPlan().getTargetVisitorCount(), median, ratio, similarResponses);
    }

    @Transactional(readOnly = true)
    public DemandFitResponse getDemandFit(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository.findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.DEMAND_FIT)
                .orElseThrow(() -> new ResourceNotFoundException("DEMAND_FIT item not found: " + analysisId));
        DemandFitAnalyzer.Result result = demandFitAnalyzer.fromSnapshots(analysis.getFestivalPlan(),
                festivalAnalysisDemandRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()),
                festivalAnalysisAccessibilityRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElse(null));
        return new DemandFitResponse(item.getItemType(), item.getScore(), result.regionalDemand(), result.seasonalDemand(), result.accessibility());
    }

    @Transactional(readOnly = true)
    public WeatherRiskResponse getWeatherRisk(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository.findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.WEATHER_RISK)
                .orElseThrow(() -> new ResourceNotFoundException("WEATHER_RISK item not found: " + analysisId));
        FestivalAnalysisWeatherRiskSnapshot snapshot = weatherRiskSnapshotRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("WEATHER_RISK snapshot not found: " + analysisId));
        try {
            return objectMapper.readValue(snapshot.getResultJson(), WeatherRiskResponse.class);
        } catch (Exception exception) {
            throw new AnalysisExecutionException("WEATHER_RISK snapshot parsing failed: " + analysisId, exception);
        }
    }

    @Transactional(readOnly = true)
    public ConflictRiskResponse getConflictRisk(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository.findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.CONFLICT_RISK)
                .orElseThrow(() -> new ResourceNotFoundException("CONFLICT_RISK item not found: " + analysisId));
        FestivalAnalysisConflict snapshot = festivalAnalysisConflictRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("CONFLICT_RISK snapshot not found: " + analysisId));
        List<ConflictRiskResponse.Event> events = festivalAnalysisConflictEventRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByEventYearAscStartDateAsc(item.getFestivalAnalysisItemId()).stream()
                .map(e -> new ConflictRiskResponse.Event(e.getFestival() == null ? null : e.getFestival().getFestivalId(), e.getEventName(), e.getEventYear(), e.getSido(), e.getSigungu(), e.getRegionRelation(), e.getStartDate(), e.getEndDate(), e.getEventBasis(), e.getConflictType(), e.getOverlapDays(), e.getSameTheme(), e.getVisitorCount())).toList();
        return new ConflictRiskResponse(item.getItemType(), item.getScore(), new ConflictRiskResponse.ConflictRisk(
                new ConflictRiskResponse.TargetPeriod(snapshot.getTargetStartDate(), snapshot.getTargetEndDate()),
                new ConflictRiskResponse.HistoryPeriod(snapshot.getHistoryStartYear(), snapshot.getHistoryEndYear()),
                snapshot.getDirectOverlapCount(), snapshot.getNearbyPeriodCount(), snapshot.getHistoricalSamePeriodCount(),
                snapshot.getSameRegionCount(), events));
    }

    private void saveConflictSnapshot(FestivalAnalysisItem item, FestivalPlan plan, likelion.festivalscope.analysis.conflict.ScheduleConflictAnalyzer.Result result) {
        festivalAnalysisConflictRepository.save(FestivalAnalysisConflict.builder().festivalAnalysisItem(item)
                .targetStartDate(plan.getStartDate()).targetEndDate(plan.getEndDate())
                .historyStartYear(result.historyStartYear()).historyEndYear(result.historyEndYear())
                .directOverlapCount((int) result.count(ConflictType.DIRECT_OVERLAP))
                .nearbyPeriodCount((int) result.count(ConflictType.NEARBY_PERIOD))
                .historicalSamePeriodCount((int) result.count(ConflictType.HISTORICAL_SAME_PERIOD))
                .sameRegionCount(result.candidates().size())
                .neighborRegionCount(0).build());
        List<FestivalAnalysisConflictEvent> events = result.candidates().stream().map(c -> {
            FestivalHistory h = c.history(); Festival f = h.getFestival();
            return FestivalAnalysisConflictEvent.builder().festivalAnalysisItem(item).festival(f).eventName(f.getFestivalName())
                    .eventYear(h.getYear()).sido(f.getSido()).sigungu(f.getSigungu()).startDate(h.getStartDate()).endDate(h.getEndDate())
                    .eventBasis(c.eventBasis()).conflictType(c.conflictType()).regionRelation(c.regionRelation()).overlapDays(c.overlapDays())
                    .visitorCount(h.getVisitorCount()).sameTheme(null).distanceKm(null).build();
        }).toList();
        festivalAnalysisConflictEventRepository.saveAll(events);
    }

    @Transactional(readOnly = true)
    public WeatherRiskResponse testWeatherRisk(Long planId) {
        FestivalPlan plan = festivalPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("FestivalPlan not found: " + planId));
        return weatherRiskAnalyzer.analyze(plan);
    }

    private void saveWeatherRiskSnapshot(FestivalAnalysisItem item, WeatherRiskResponse response) {
        try {
            weatherRiskSnapshotRepository.save(FestivalAnalysisWeatherRiskSnapshot.builder()
                    .festivalAnalysisItem(item)
                    .resultJson(objectMapper.writeValueAsString(response))
                    .build());
        } catch (Exception exception) {
            throw new AnalysisExecutionException("WEATHER_RISK snapshot save failed", exception);
        }
    }

    private void saveDemandSnapshots(FestivalAnalysisItem item, FestivalPlan plan, DemandFitAnalyzer.Result result) {
        List<FestivalAnalysisDemand> rows = new ArrayList<>();
        result.regionalYears().forEach(row -> rows.add(FestivalAnalysisDemand.builder().festivalAnalysisItem(item).demandType(DemandType.REGIONAL).regionCode(row.code()).sido(plan.getSido()).sigungu(row.name()).statYear(row.year()).visitorCount(row.value()).build()));
        result.dailyRecords().forEach(row -> rows.add(FestivalAnalysisDemand.builder().festivalAnalysisItem(item).demandType(DemandType.SEASONAL).regionCode(row.regionCode()).sido(plan.getSido()).sigungu(row.regionName()).statYear(row.date().getYear()).statMonth(row.date().getMonthValue()).statDay(row.date().getDayOfMonth()).visitorCount(row.visitorCount()).build()));
            festivalAnalysisDemandRepository.saveAll(rows);
        DemandFitResponse.Bus bus = result.accessibility().bus(); DemandFitResponse.Rail rail = result.accessibility().rail();
        festivalAnalysisAccessibilityRepository.save(FestivalAnalysisAccessibility.builder().festivalAnalysisItem(item).nearestBusStopName(bus.nearestStopName()).nearestBusStopDistanceM(bus.nearestStopDistanceM()).busStopCount500m(bus.stopCount500m()).busStopCount1km(bus.stopCount1km()).busRouteCount(bus.routeCount()).railAvailable(rail.available()).nearestStationName(rail.nearestStationName()).nearestStationDistanceM(rail.nearestStationDistanceM()).build());
    }
    private FestivalAnalysis getAnalysisEntity(Long analysisId) {
        return festivalAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("遺꾩꽍??李얠쓣 ???놁뒿?덈떎: " + analysisId));
    }

    @Transactional(readOnly = true)
    public TrendFitResponse getTrendFit(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository
                .findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.TREND_FIT)
                .orElseThrow(() -> new ResourceNotFoundException("TREND_FIT item not found: " + analysisId));
        List<FestivalAnalysisTrendKeyword> yearlyRows = festivalAnalysisTrendKeywordRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
                        item.getFestivalAnalysisItemId(), PeriodType.YEARLY);
        List<FestivalAnalysisTrendKeyword> monthlyRows = festivalAnalysisTrendKeywordRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
                        item.getFestivalAnalysisItemId(), PeriodType.MONTHLY);
        if (yearlyRows.isEmpty()) {
            throw new AnalysisExecutionException("TREND_FIT yearly snapshot not found: " + analysisId);
        }

        Map<String, List<FestivalAnalysisTrendKeyword>> yearlyByKeyword = yearlyRows.stream()
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getKeyword, LinkedHashMap::new, Collectors.toList()));
        List<TrendFitResponse.KeywordTrend> keywordTrends = yearlyByKeyword.entrySet().stream()
                .map(entry -> toKeywordTrend(entry.getKey(), entry.getValue()))
                .toList();

        Map<Integer, List<BigDecimal>> valuesByYear = yearlyRows.stream()
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getPeriodYear,
                        TreeMap::new, Collectors.mapping(FestivalAnalysisTrendKeyword::getInterestValue, Collectors.toList())));
        List<TrendFitResponse.YearlyInterest> integratedInterest = valuesByYear.entrySet().stream()
                .map(entry -> new TrendFitResponse.YearlyInterest(entry.getKey(), average(entry.getValue())))
                .toList();
        List<TrendFitResponse.YearlyGrowthRate> integratedGrowth = growthRates(integratedInterest);

        List<TrendFitResponse.PreviousYearAroundEventPeriod> aroundEvent = createAroundEventResponse(
                analysis, yearlyByKeyword.keySet(), monthlyRows);
        return new TrendFitResponse(
                item.getItemType(), item.getScore(),
                new TrendFitResponse.IntegratedTrend(integratedInterest, integratedGrowth),
                keywordTrends, aroundEvent);
    }

    private void saveTrendKeywords(FestivalAnalysisItem item, TrendAnalysisResult result) {
        List<FestivalAnalysisTrendKeyword> rows = new ArrayList<>();
        result.monthlyData().stream()
                .filter(data -> data.interestValue() != null)
                .map(data -> FestivalAnalysisTrendKeyword.builder()
                        .festivalAnalysisItem(item)
                        .keyword(data.keyword())
                        .periodType(PeriodType.MONTHLY)
                        .periodYear(data.month().getYear())
                        .periodMonth(data.month().getMonthValue())
                        .interestValue(data.interestValue())
                        .build())
                .forEach(rows::add);
        result.yearlyData().stream()
                .map(data -> FestivalAnalysisTrendKeyword.builder()
                        .festivalAnalysisItem(item)
                        .keyword(data.keyword())
                        .periodType(PeriodType.YEARLY)
                        .periodYear(data.year())
                        .interestValue(data.interestValue())
                        .build())
                .forEach(rows::add);
        festivalAnalysisTrendKeywordRepository.saveAll(rows);
    }

    private TrendFitResponse.KeywordTrend toKeywordTrend(String keyword, List<FestivalAnalysisTrendKeyword> rows) {
        List<TrendFitResponse.YearlyInterest> interest = rows.stream()
                .map(row -> new TrendFitResponse.YearlyInterest(row.getPeriodYear(), row.getInterestValue()))
                .toList();
        return new TrendFitResponse.KeywordTrend(keyword, interest, growthRates(interest));
    }

    private List<TrendFitResponse.YearlyGrowthRate> growthRates(List<TrendFitResponse.YearlyInterest> values) {
        List<TrendFitResponse.YearlyGrowthRate> result = new ArrayList<>();
        for (int index = 1; index < values.size(); index++) {
            TrendFitResponse.YearlyInterest previous = values.get(index - 1);
            TrendFitResponse.YearlyInterest current = values.get(index);
            result.add(new TrendFitResponse.YearlyGrowthRate(
                    previous.year(), current.year(), calculateGrowth(previous.interest(), current.interest())));
        }
        return result;
    }

    private List<TrendFitResponse.PreviousYearAroundEventPeriod> createAroundEventResponse(
            FestivalAnalysis analysis, Set<String> keywords, List<FestivalAnalysisTrendKeyword> monthlyRows) {
        if (analysis.getFestivalPlan().getStartDate() == null) return List.of();
        YearMonth eventMonth = YearMonth.from(analysis.getFestivalPlan().getStartDate()).minusYears(1);
        YearMonth start = eventMonth.minusMonths(3);
        YearMonth end = eventMonth.plusMonths(3);
        return keywords.stream().map(keyword -> {
            Map<YearMonth, BigDecimal> values = monthlyRows.stream()
                    .filter(row -> row.getKeyword().equals(keyword))
                    .collect(Collectors.toMap(row -> YearMonth.of(row.getPeriodYear(), row.getPeriodMonth()),
                            FestivalAnalysisTrendKeyword::getInterestValue, (first, second) -> first));
            List<TrendFitResponse.MonthlyInterest> monthlyInterest = new ArrayList<>();
            for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
                monthlyInterest.add(new TrendFitResponse.MonthlyInterest(month.toString(), values.get(month)));
            }
            return new TrendFitResponse.PreviousYearAroundEventPeriod(keyword, monthlyInterest);
        }).toList();
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGrowth(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
    private void replaceItemScore(FestivalAnalysisItem item, BigDecimal score) {
        festivalAnalysisItemRepository.save(FestivalAnalysisItem.builder()
                .festivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .festivalAnalysis(item.getFestivalAnalysis())
                .itemType(item.getItemType())
                .score(score)
                .build());
    }

    private void replaceAnalysisStatus(FestivalAnalysis analysis, BigDecimal totalScore,
                                       AnalysisStatus status, LocalDateTime completedAt) {
        festivalAnalysisRepository.save(FestivalAnalysis.builder()
                .festivalAnalysisId(analysis.getFestivalAnalysisId())
                .festivalPlan(analysis.getFestivalPlan())
                .analysisVersion(analysis.getAnalysisVersion())
                .totalScore(totalScore)
                .analysisStatus(status)
                .startedAt(analysis.getStartedAt())
                .completedAt(completedAt)
                .build());
    }

    private BigDecimal calculateMedian(List<Long> sortedValues) {
        if (sortedValues.isEmpty()) throw new AnalysisExecutionException("?좎궗 異뺤젣 諛⑸Ц媛??곗씠?곌? ?놁뒿?덈떎.");
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) return BigDecimal.valueOf(sortedValues.get(middle));
        return BigDecimal.valueOf(sortedValues.get(middle - 1))
                .add(BigDecimal.valueOf(sortedValues.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
