package likelion.festivalscope.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.analysis.analyzer.TargetVisitorAnalyzer;
import likelion.festivalscope.analysis.analyzer.TrendAnalysisResult;
import likelion.festivalscope.analysis.analyzer.TrendFitAnalyzer;
import likelion.festivalscope.analysis.analyzer.DemandFitAnalyzer;
import likelion.festivalscope.analysis.analyzer.TourismLinkageAnalyzer;
import likelion.festivalscope.analysis.dto.response.DemandFitResponse;
import likelion.festivalscope.analysis.weather.WeatherRiskAnalyzer;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.analysis.conflict.ScheduleConflictAnalyzer;
import likelion.festivalscope.analysis.dto.response.ConflictRiskResponse;
import likelion.festivalscope.analysis.dto.response.TourismLinkageResponse;
import likelion.festivalscope.analysis.dto.response.FestivalAnalysisResponse;
import likelion.festivalscope.analysis.dto.response.FinalReportResponse;
import likelion.festivalscope.analysis.dto.response.TargetVisitorResponse;
import likelion.festivalscope.analysis.dto.response.TrendFitResponse;
import likelion.festivalscope.analysis.dto.response.ResultInterpretation;
import likelion.festivalscope.analysis.dto.response.AnalysisItemSummaryResponse;
import likelion.festivalscope.analysis.dto.response.ChartDataResponse;
import likelion.festivalscope.analysis.dto.response.ChartResponse;
import likelion.festivalscope.analysis.dto.response.InterpretationDecision;
import likelion.festivalscope.analysis.dto.response.InterpretationMetric;
import likelion.festivalscope.analysis.dto.response.MetricResponse;
import likelion.festivalscope.analysis.dto.response.PrimaryMetricResponse;
import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.plan.entity.*;
import likelion.festivalscope.festival.entity.*;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.AnalysisStatus;
import likelion.festivalscope.external.tourism.TourApiClient;
import likelion.festivalscope.analysis.repository.*;
import likelion.festivalscope.analysis.recommendation.service.RecommendationService;
import likelion.festivalscope.analysis.dto.response.RecommendationResponse;
import likelion.festivalscope.plan.repository.*;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;
import likelion.festivalscope.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FestivalAnalysisService {
    private final FestivalPlanRepository festivalPlanRepository;
    private final FestivalPlanThemeRepository festivalPlanThemeRepository;
    private final FestivalAnalysisRepository festivalAnalysisRepository;
    private final FestivalAnalysisItemRepository festivalAnalysisItemRepository;
    private final FestivalAnalysisSimilarRepository festivalAnalysisSimilarRepository;
    private final FestivalAnalysisTargetVisitorRepository festivalAnalysisTargetVisitorRepository;
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
    private final FestivalAnalysisTourismLinkageRepository festivalAnalysisTourismLinkageRepository;
    private final FestivalAnalysisPoiRepository festivalAnalysisPoiRepository;
    private final TourismLinkageAnalyzer tourismLinkageAnalyzer;
    private final RecommendationService recommendationService;
    private final RecommendationQueryService recommendationQueryService;
    private final AnalysisScoreService analysisScoreService;
    private final FestivalAnalysisInterpretationSnapshotRepository festivalAnalysisInterpretationSnapshotRepository;
    private final ResultInterpretationService resultInterpretationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void runSimilarFestival(Long id) {
        FestivalAnalysis analysis = getAnalysisEntity(id); FestivalPlan plan = analysis.getFestivalPlan();
        FestivalAnalysisItem item = item(id, AnalysisItemType.TARGET_VISITOR);
        TargetVisitorAnalyzer.Result result = targetVisitorAnalyzer.analyze(plan, festivalPlanThemeRepository.findAllByFestivalPlan_FestivalPlanId(plan.getFestivalPlanId()));
        for (int index = 0; index < result.candidates().size(); index++) {
            TargetVisitorAnalyzer.Candidate candidate = result.candidates().get(index);
            festivalAnalysisSimilarRepository.save(FestivalAnalysisSimilar.builder().festivalAnalysisItem(item)
                    .festival(candidate.festival()).festivalHistory(candidate.history())
                    .festivalName(candidate.festival() == null ? candidate.history().getFestivalNameRaw() : candidate.festival().getFestivalName())
                    .year(candidate.history().getYear()).budget(candidate.history().getBudget()).visitorCount(candidate.history().getVisitorCount())
                    .similarityScore(candidate.similarityScore()).themeSimilarity(candidate.themeSimilarity()).regionSimilarity(candidate.regionSimilarity())
                    .periodSimilarity(candidate.periodSimilarity()).comparisonType(candidate.comparisonType()).rankOrder(index + 1).build());
        }
        saveTargetVisitorSnapshot(item, plan, result);
    }
    @Transactional public void runTrendFit(Long id) { saveTrendKeywords(item(id, AnalysisItemType.TREND_FIT), trendFitAnalyzer.analyze(getAnalysisEntity(id).getFestivalPlan())); }
    @Transactional public void runDemandFit(Long id) { FestivalPlan p=getAnalysisEntity(id).getFestivalPlan(); saveDemandSnapshots(item(id, AnalysisItemType.DEMAND_FIT), p, demandFitAnalyzer.analyze(p)); }
    @Transactional public void runCompetitionRisk(Long id) { FestivalPlan p=getAnalysisEntity(id).getFestivalPlan(); saveConflictSnapshot(item(id, AnalysisItemType.CONFLICT_RISK), p, scheduleConflictAnalyzer.analyze(p)); }
    @Transactional public void runWeatherRisk(Long id) { saveWeatherRiskSnapshot(item(id, AnalysisItemType.WEATHER_RISK), weatherRiskAnalyzer.analyze(getAnalysisEntity(id).getFestivalPlan())); }
    @Transactional public void runTourismLinkage(Long id) { saveTourismLinkageSnapshot(item(id, AnalysisItemType.TOURISM_LINKAGE), tourismLinkageAnalyzer.analyze(getAnalysisEntity(id).getFestivalPlan())); }
    @Transactional public void finalizeAnalysis(Long id) { FestivalAnalysis a=getAnalysisEntity(id); recommendationService.replaceForAnalysis(a); replaceAnalysisStatus(a, saveInterpretationSnapshots(a), AnalysisStatus.COMPLETED, LocalDateTime.now()); }
    public void markCompleted(Long id) { }
    @Transactional public void markFailed(Long id) { replaceAnalysisStatus(getAnalysisEntity(id), null, AnalysisStatus.FAILED, LocalDateTime.now()); }
    private FestivalAnalysisItem item(Long id, AnalysisItemType type) { return festivalAnalysisItemRepository.findByFestivalAnalysis_FestivalAnalysisIdAndItemType(id, type).orElseThrow(); }

    @Transactional(readOnly = true)
    public Page<likelion.festivalscope.analysis.dto.response.AnalysisListResponse> getAnalysisList(Pageable pageable) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long userId)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return festivalAnalysisRepository.findCompletedAnalysisList(userId, AnalysisStatus.COMPLETED, pageable)
                .map(likelion.festivalscope.analysis.dto.response.AnalysisListResponse::from);
    }

    @Transactional
    public Long createAnalysis(Long planId) {
        FestivalPlan plan = festivalPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Festival plan not found: " + planId));
        verifyOwner(plan.getUser().getUserId());
        FestivalAnalysis analysis = festivalAnalysisRepository.save(FestivalAnalysis.builder()
                .festivalPlan(plan).analysisVersion("v1.0").analysisStatus(AnalysisStatus.PROCESSING)
                .startedAt(LocalDateTime.now()).build());
        festivalAnalysisItemRepository.saveAll(Arrays.stream(AnalysisItemType.values())
                .map(itemType -> FestivalAnalysisItem.builder().festivalAnalysis(analysis).itemType(itemType).build()).toList());
        return analysis.getFestivalAnalysisId();
    }

    @Transactional(noRollbackFor = AnalysisExecutionException.class)
    public Long execute(Long planId) {
        FestivalPlan plan = festivalPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("축제 기획안을 찾을 수 없습니다: " + planId));
        verifyOwner(plan.getUser().getUserId());
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
                        .festivalName(candidate.festival() == null ? candidate.history().getFestivalNameRaw() : candidate.festival().getFestivalName())
                        .year(candidate.history().getYear())
                        .budget(candidate.history().getBudget())
                        .visitorCount(candidate.history().getVisitorCount())
                        .budget(candidate.history().getBudget())
                        .similarityScore(candidate.similarityScore())
                        .themeSimilarity(candidate.themeSimilarity())
                        .regionSimilarity(candidate.regionSimilarity())
                        .periodSimilarity(candidate.periodSimilarity())
                        .comparisonType(candidate.comparisonType())
                        .rankOrder(index + 1)
                        .build());
            }
            saveTargetVisitorSnapshot(targetVisitorItem, plan, result);

            FestivalAnalysisItem tourismItem = items.stream()
                    .filter(item -> item.getItemType() == AnalysisItemType.TOURISM_LINKAGE)
                    .findFirst().orElseThrow();
            saveTourismLinkageSnapshot(tourismItem, tourismLinkageAnalyzer.analyze(plan));

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

            recommendationService.replaceForAnalysis(analysis);
            BigDecimal overallScore = saveInterpretationSnapshots(analysis);

            replaceAnalysisStatus(analysis, overallScore, AnalysisStatus.COMPLETED, LocalDateTime.now());
            return analysis.getFestivalAnalysisId();
        } catch (Exception exception) {
            replaceAnalysisStatus(analysis, null, AnalysisStatus.FAILED, LocalDateTime.now());
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            if (exception instanceof AnalysisExecutionException analysisException) {
                throw analysisException;
            }
            throw new AnalysisExecutionException("축제 분석 실행에 실패했습니다.", exception);
        }
    }

    @Transactional(readOnly = true)
    public TourismLinkageResponse getTourismLinkage(Long analysisId) {
        FestivalAnalysisItem item = festivalAnalysisItemRepository
                .findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.TOURISM_LINKAGE)
                .orElseThrow(() -> new ResourceNotFoundException("TOURISM_LINKAGE item not found: " + analysisId));
        FestivalAnalysisTourismLinkage snapshot = festivalAnalysisTourismLinkageRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("TOURISM_LINKAGE snapshot not found: " + analysisId));
        List<TourismLinkageResponse.Poi> pois = festivalAnalysisPoiRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByDistanceMAsc(item.getFestivalAnalysisItemId())
                .stream().map(this::toTourismPoi).toList();
        List<TourismLinkageResponse.Poi> culture = pois.stream().filter(p -> p.poiType() == PoiType.TOURIST_ATTRACTION || p.poiType() == PoiType.CULTURAL_FACILITY).toList();
        List<TourismLinkageResponse.Poi> commerce = pois.stream().filter(p -> p.poiType() == PoiType.RESTAURANT || p.poiType() == PoiType.SHOPPING).toList();
        List<TourismLinkageResponse.Poi> accommodation = pois.stream().filter(p -> p.poiType() == PoiType.ACCOMMODATION).toList();
        TourismLinkageResponse.PoiSummary poiSummary = new TourismLinkageResponse.PoiSummary(
                new TourismLinkageResponse.RangeCount(
                        snapshot.getTourismCultureWithin3kmCount() + snapshot.getFoodShoppingWithin3kmCount() + snapshot.getAccommodationWithin3kmCount(),
                        snapshot.getTourismCultureWithin3kmCount(), snapshot.getFoodShoppingWithin3kmCount(), snapshot.getAccommodationWithin3kmCount()),
                new TourismLinkageResponse.RangeCount(
                        snapshot.getTourismCultureBetween3And5kmCount() + snapshot.getFoodShoppingBetween3And5kmCount() + snapshot.getAccommodationBetween3And5kmCount(),
                        snapshot.getTourismCultureBetween3And5kmCount(), snapshot.getFoodShoppingBetween3And5kmCount(), snapshot.getAccommodationBetween3And5kmCount()),
                new TourismLinkageResponse.RangeCount(snapshot.getTotalCandidatePoiCount(), snapshot.getTourismCultureCount(), snapshot.getFoodShoppingCount(), snapshot.getAccommodationCount()));
        TourismLinkageResponse.RegionalIndicators indicators = new TourismLinkageResponse.RegionalIndicators(
                indicator(snapshot.getResourceDemandBaseYm(), snapshot.getResourceDemandCode(), snapshot.getResourceDemandName(), snapshot.getResourceDemandValue()),
                indicator(snapshot.getConsumptionIntensityBaseYm(), snapshot.getConsumptionIntensityCode(), snapshot.getConsumptionIntensityName(), snapshot.getConsumptionIntensityValue()),
                indicator(snapshot.getStayIntensityBaseYm(), snapshot.getStayIntensityCode(), snapshot.getStayIntensityName(), snapshot.getStayIntensityValue()));
        return new TourismLinkageResponse(item.getItemType(), item.getScore(), new TourismLinkageResponse.TourismLinkage(
                snapshot.getTotalCandidatePoiCount(), snapshot.getTourismCultureCount(), snapshot.getFoodShoppingCount(),
                snapshot.getAccommodationCount(), snapshot.getTourismLinkageSummary(), snapshot.getConsumptionLinkageSummary(),
                snapshot.getStayLinkageSummary(), poiSummary, indicators, culture.stream().limit(5).toList(),
                commerce.stream().limit(5).toList(), accommodation.stream().limit(5).toList(),
                groups(culture), groups(commerce), groups(accommodation)),
                recommendationQueryService.findForItem(item), resultInterpretationService.interpretTourismLinkage(snapshot).interpretation());
    }

    private TourismLinkageResponse.RangeCount rangeCount(List<TourismLinkageResponse.Poi> culture, List<TourismLinkageResponse.Poi> commerce, List<TourismLinkageResponse.Poi> accommodation, PoiDistanceRange range) {
        int c = (int) culture.stream().filter(p -> p.distanceRange() == range).count();
        int f = (int) commerce.stream().filter(p -> p.distanceRange() == range).count();
        int a = (int) accommodation.stream().filter(p -> p.distanceRange() == range).count();
        return new TourismLinkageResponse.RangeCount(c + f + a, c, f, a);
    }

    private TourismLinkageResponse.Indicator indicator(String baseYm, String code, String name, BigDecimal value) {
        return value == null ? null : new TourismLinkageResponse.Indicator(baseYm, code, name, value);
    }

    private TourismLinkageResponse.PoiRangeGroups groups(List<TourismLinkageResponse.Poi> pois) {
        return new TourismLinkageResponse.PoiRangeGroups(
                pois.stream().filter(p -> p.distanceRange() == PoiDistanceRange.WITHIN_3KM).limit(5).toList(),
                pois.stream().filter(p -> p.distanceRange() == PoiDistanceRange.BETWEEN_3_AND_5KM).limit(5).toList());
    }

    private TourismLinkageResponse.Poi toTourismPoi(FestivalAnalysisPoi poi) {
        return new TourismLinkageResponse.Poi(poi.getContentId(), poi.getPoiName(), poi.getContentTypeId(), poi.getPoiType(),
                poi.getLinkageType(), poi.getDistanceM(), poi.getDistanceRange(), poi.getLatitude(), poi.getLongitude(), poi.getAddress(), poi.getImageUrl());
    }

    @Transactional(readOnly = true)
    public FestivalAnalysisResponse getAnalysis(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        List<AnalysisItemSummaryResponse> items = festivalAnalysisItemRepository
                .findAllByFestivalAnalysis_FestivalAnalysisIdOrderByFestivalAnalysisItemIdAsc(analysisId)
                .stream()
                .map(item -> toAnalysisItemSummary(analysis, item))
                .toList();
        return new FestivalAnalysisResponse(
                analysis.getFestivalAnalysisId(),
                analysis.getFestivalPlan().getFestivalPlanId(),
                analysis.getFestivalPlan().getFestivalName(),
                analysis.getTotalScore(),
                analysis.getScoreGrade(),
                analysis.getAnalysisStatus(),
                analysis.getCreatedAt(),
                items);
    }

    @Transactional(readOnly = true)
    public FinalReportResponse getFinalReport(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisResponse summary = getAnalysis(analysisId);
        List<RecommendationResponse> recommendations = recommendationQueryService.findAll(analysis.getFestivalAnalysisId());
        return new FinalReportResponse(summary, recommendations);
    }

    private AnalysisItemSummaryResponse toAnalysisItemSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        return switch (item.getItemType()) {
            case TARGET_VISITOR -> targetVisitorSummary(analysis, item);
            case TREND_FIT -> trendSummary(analysis, item);
            case DEMAND_FIT -> demandSummary(analysis, item);
            case CONFLICT_RISK -> conflictSummary(analysis, item);
            case WEATHER_RISK -> weatherSummary(analysis, item);
            case TOURISM_LINKAGE -> tourismSummary(analysis, item);
        };
    }

    private BigDecimal saveInterpretationSnapshots(FestivalAnalysis analysis) {
        List<FestivalAnalysisItem> items = festivalAnalysisItemRepository
                .findAllByFestivalAnalysis_FestivalAnalysisIdOrderByFestivalAnalysisItemIdAsc(analysis.getFestivalAnalysisId())
                .stream().toList();
        Map<AnalysisItemType, BigDecimal> scores = new EnumMap<>(AnalysisItemType.class);
        for (FestivalAnalysisItem item : items) {
            InterpretationDecision decision = decisionFor(analysis, item);
            festivalAnalysisInterpretationSnapshotRepository.save(toInterpretationSnapshot(analysis, item, decision));
            DemandFitAnalyzer.Result demandResult = item.getItemType() == AnalysisItemType.DEMAND_FIT
                    ? demandResult(item, analysis.getFestivalPlan()) : null;
            BigDecimal score = analysisScoreService.score(
                    analysis.getFestivalPlan(), decision, demandResult,
                    item.getItemType() == AnalysisItemType.DEMAND_FIT);
            if (score != null) {
                replaceItemScore(item, score);
                scores.put(item.getItemType(), score);
            }
        }
        BigDecimal targetScore = scores.get(AnalysisItemType.TARGET_VISITOR);
        BigDecimal trendScore = scores.get(AnalysisItemType.TREND_FIT);
        BigDecimal demandScore = scores.get(AnalysisItemType.DEMAND_FIT);
        return analysisScoreService.overall(targetScore, trendScore, demandScore);
    }

    private FestivalAnalysisInterpretationSnapshot toInterpretationSnapshot(
            FestivalAnalysis analysis, FestivalAnalysisItem item, InterpretationDecision decision) {
        FestivalAnalysisInterpretationSnapshot.FestivalAnalysisInterpretationSnapshotBuilder builder =
                FestivalAnalysisInterpretationSnapshot.builder()
                        .festivalAnalysisItem(item)
                        .status(decision.status())
                        .statusLevel(decision.statusLevel())
                        .summary(decision.interpretation() == null ? "데이터 없음" : decision.interpretation().summary())
                        .detail(decision.interpretation() == null ? "데이터 없음" : decision.interpretation().detail());
        switch (item.getItemType()) {
            case TARGET_VISITOR -> builder.comparisonMedian(metricDecimal(decision, "comparisonMedian"))
                    .gapRate(metricDecimal(decision, "gapRate"))
                    .targetRatio(metricDecimal(decision, "targetRatio"));
            case TREND_FIT -> builder.latestGrowthRate(metricDecimal(decision, "latestGrowthRate"))
                    .decliningKeywordRate(metricDecimal(decision, "decliningKeywordRate"))
                    .eventPeriodGap(metricDecimal(decision, "eventPeriodGap"));
            case DEMAND_FIT -> builder.regionPercentile(metricDecimal(decision, "regionPercentile"))
                    .monthPercentile(metricDecimal(decision, "monthPercentile"))
                    .eventMonthRank(metricInteger(decision, "eventMonthRank"))
                    .currentWeekRank(metricInteger(decision, "currentWeekRank"));
            case CONFLICT_RISK -> builder.directOverlapCount(metricInteger(decision, "directOverlapCount"))
                    .nearbyPeriodCount(metricInteger(decision, "nearbyPeriodCount"))
                    .possibleConflictCount(metricInteger(decision, "possibleConflictCount"))
                    .historicalEventYears(metricInteger(decision, "historicalEventYears"))
                    .historyYears(metricInteger(decision, "historyYears"));
            case WEATHER_RISK -> builder.rainOccurrenceRate(metricDecimal(decision, "rainOccurrenceRate"))
                    .temperatureType(enumValue(FestivalAnalysisInterpretationSnapshot.TemperatureType.class, metricValue(decision, "temperatureType")))
                    .temperatureOccurrenceRate(metricDecimal(decision, "temperatureOccurrenceRate"))
                    .windOccurrenceRate(metricDecimal(decision, "windOccurrenceRate"))
                    .spaceType(enumValue(VenueType.class, metricValue(decision, "spaceType")))
                    .highRiskCount(metricInteger(decision, "highRiskCount"))
                    .moderateRiskCount(metricInteger(decision, "moderateRiskCount"));
            case TOURISM_LINKAGE -> builder.totalPoiWithin5km(metricInteger(decision, "totalPoiWithin5km"))
                    .tourismCultureWithin3km(metricInteger(decision, "tourismCultureWithin3km"))
                    .foodShoppingWithin3km(metricInteger(decision, "foodShoppingWithin3km"))
                    .accommodationWithin5km(metricInteger(decision, "accommodationWithin5km"))
                    .highPotentialCount(metricInteger(decision, "highPotentialCount"))
                    .lowPotentialCount(metricInteger(decision, "lowPotentialCount"));
        }
        return builder.build();
    }

    private InterpretationDecision storedDecisionOrCompute(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        return festivalAnalysisInterpretationSnapshotRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .map(this::toDecision)
                .orElseGet(() -> decisionFor(analysis, item));
    }

    private InterpretationDecision toDecision(FestivalAnalysisInterpretationSnapshot snapshot) {
        FestivalAnalysisInterpretationSnapshot.TemperatureType temperatureType = snapshot.getTemperatureType();
        List<InterpretationMetric> metrics = switch (snapshot.getFestivalAnalysisItem().getItemType()) {
            case TARGET_VISITOR -> List.of(metricEntry("comparisonMedian", snapshot.getComparisonMedian()), metricEntry("gapRate", snapshot.getGapRate()), metricEntry("targetRatio", snapshot.getTargetRatio()));
            case TREND_FIT -> List.of(metricEntry("latestGrowthRate", snapshot.getLatestGrowthRate()), metricEntry("decliningKeywordRate", snapshot.getDecliningKeywordRate()), metricEntry("eventPeriodGap", snapshot.getEventPeriodGap()));
            case DEMAND_FIT -> List.of(metricEntry("regionPercentile", snapshot.getRegionPercentile()), metricEntry("monthPercentile", snapshot.getMonthPercentile()), metricEntry("eventMonthRank", snapshot.getEventMonthRank()), metricEntry("currentWeekRank", snapshot.getCurrentWeekRank()));
            case CONFLICT_RISK -> List.of(metricEntry("directOverlapCount", snapshot.getDirectOverlapCount()), metricEntry("nearbyPeriodCount", snapshot.getNearbyPeriodCount()), metricEntry("possibleConflictCount", snapshot.getPossibleConflictCount()), metricEntry("historicalEventYears", snapshot.getHistoricalEventYears()), metricEntry("historyYears", snapshot.getHistoryYears()));
            case WEATHER_RISK -> List.of(metricEntry("rainOccurrenceRate", snapshot.getRainOccurrenceRate()), metricEntry("temperatureType", temperatureType), metricEntry("temperatureOccurrenceRate", snapshot.getTemperatureOccurrenceRate()), metricEntry("windOccurrenceRate", snapshot.getWindOccurrenceRate()), metricEntry("spaceType", snapshot.getSpaceType()), metricEntry("highRiskCount", snapshot.getHighRiskCount()), metricEntry("moderateRiskCount", snapshot.getModerateRiskCount()));
            case TOURISM_LINKAGE -> List.of(metricEntry("totalPoiWithin5km", snapshot.getTotalPoiWithin5km()), metricEntry("tourismCultureWithin3km", snapshot.getTourismCultureWithin3km()), metricEntry("foodShoppingWithin3km", snapshot.getFoodShoppingWithin3km()), metricEntry("accommodationWithin5km", snapshot.getAccommodationWithin5km()), metricEntry("highPotentialCount", snapshot.getHighPotentialCount()), metricEntry("lowPotentialCount", snapshot.getLowPotentialCount()));
        };
        return new InterpretationDecision(snapshot.getStatus(), snapshot.getStatusLevel(),
                new ResultInterpretation(snapshot.getSummary(), snapshot.getDetail()), metrics);
    }

    private InterpretationMetric metricEntry(String key, Object value) { return new InterpretationMetric(key, value); }

    private InterpretationDecision decisionFor(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        return switch (item.getItemType()) {
            case TARGET_VISITOR -> {
                FestivalAnalysisTargetVisitor snapshot = festivalAnalysisTargetVisitorRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElseThrow();
                yield resultInterpretationService.interpretTargetVisitor(analysis.getFestivalPlan(), snapshot.getTargetVisitorCount(),
                        festivalAnalysisSimilarRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByRankOrderAsc(item.getFestivalAnalysisItemId()));
            }
            case TREND_FIT -> {
                List<FestivalAnalysisTrendKeyword> yearly = trendRows(item, PeriodType.YEARLY);
                yield resultInterpretationService.interpretTrendFit(analysis, yearly, trendRows(item, PeriodType.MONTHLY));
            }
            case DEMAND_FIT -> resultInterpretationService.interpretDemandFit(analysis.getFestivalPlan(), demandResult(item, analysis.getFestivalPlan()));
            case CONFLICT_RISK -> resultInterpretationService.interpretConflictRisk(conflictSnapshot(item), conflictEvents(item));
            case WEATHER_RISK -> resultInterpretationService.interpretWeatherRisk(weatherResponse(item));
            case TOURISM_LINKAGE -> resultInterpretationService.interpretTourismLinkage(tourismSnapshot(item));
        };
    }

    private List<FestivalAnalysisTrendKeyword> trendRows(FestivalAnalysisItem item, PeriodType type) {
        return festivalAnalysisTrendKeywordRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(item.getFestivalAnalysisItemId(), type);
    }

    private DemandFitAnalyzer.Result demandResult(FestivalAnalysisItem item, FestivalPlan plan) {
        return demandFitAnalyzer.fromSnapshots(plan,
                festivalAnalysisDemandRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()),
                festivalAnalysisAccessibilityRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElse(null));
    }

    private FestivalAnalysisConflict conflictSnapshot(FestivalAnalysisItem item) {
        return festivalAnalysisConflictRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElseThrow();
    }

    private List<ConflictRiskResponse.Event> conflictEvents(FestivalAnalysisItem item) {
        return festivalAnalysisConflictEventRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByEventYearAscStartDateAsc(item.getFestivalAnalysisItemId()).stream()
                .map(e -> new ConflictRiskResponse.Event(e.getFestival() == null ? null : e.getFestival().getFestivalId(), e.getEventName(), e.getEventYear(), e.getSido(), e.getSigungu(), e.getRegionRelation(), e.getStartDate(), e.getEndDate(), e.getEventBasis(), e.getConflictType(), e.getOverlapDays(), e.getSameTheme(), e.getVisitorCount())).toList();
    }

    private WeatherRiskResponse weatherResponse(FestivalAnalysisItem item) {
        FestivalAnalysisWeatherRiskSnapshot snapshot = weatherRiskSnapshotRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElseThrow();
        try { return objectMapper.readValue(snapshot.getResultJson(), WeatherRiskResponse.class); }
        catch (Exception exception) { throw new AnalysisExecutionException("WEATHER_RISK snapshot parsing failed", exception); }
    }

    private FestivalAnalysisTourismLinkage tourismSnapshot(FestivalAnalysisItem item) {
        return festivalAnalysisTourismLinkageRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElseThrow();
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        return value == null ? null : Enum.valueOf(type, String.valueOf(value));
    }

    private AnalysisItemSummaryResponse targetVisitorSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        FestivalAnalysisTargetVisitor snapshot = festivalAnalysisTargetVisitorRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("TARGET_VISITOR snapshot not found: " + analysis.getFestivalAnalysisId()));
        List<FestivalAnalysisSimilar> rows = festivalAnalysisSimilarRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByRankOrderAsc(item.getFestivalAnalysisItemId());
        InterpretationDecision decision = storedDecisionOrCompute(analysis, item);
        BigDecimal target = decimal(snapshot.getTargetVisitorCount());
        BigDecimal median = metricDecimal(decision, "comparisonMedian");
        BigDecimal ratio = metricDecimal(decision, "targetRatio");
        String label = analysis.getFestivalPlan().getFestivalStatus() == FestivalStatus.NEW
                ? "유사 축제 방문객 중위값 대비" : "과거 방문 이력 중위값 대비";
        return summary(item, "목표 방문객 규모", decision,
                primary(formatMultiplier(ratio), label),
                metrics(metric("목표 방문객", formatVisitors(target)), metric("비교 기준 방문객", formatVisitors(median))),
                chart("HORIZONTAL_BAR", null, data("목표 방문객", target), data("비교 기준 방문객", median)));
    }

    private AnalysisItemSummaryResponse trendSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        List<FestivalAnalysisTrendKeyword> yearlyRows = festivalAnalysisTrendKeywordRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
                        item.getFestivalAnalysisItemId(), PeriodType.YEARLY);
        List<FestivalAnalysisTrendKeyword> monthlyRows = festivalAnalysisTrendKeywordRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
                        item.getFestivalAnalysisItemId(), PeriodType.MONTHLY);
        InterpretationDecision decision = storedDecisionOrCompute(analysis, item);
        Map<Integer, List<BigDecimal>> valuesByYear = yearlyRows.stream()
                .filter(row -> row.getPeriodYear() != null && row.getInterestValue() != null)
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getPeriodYear, TreeMap::new,
                        Collectors.mapping(FestivalAnalysisTrendKeyword::getInterestValue, Collectors.toList())));
        List<TrendFitResponse.YearlyInterest> interests = valuesByYear.entrySet().stream()
                .map(entry -> new TrendFitResponse.YearlyInterest(entry.getKey(), average(entry.getValue())))
                .toList();
        List<ChartDataResponse> chartData = interests.stream().skip(Math.max(0, interests.size() - 5))
                .map(value -> data(String.valueOf(value.year()), value.interest())).toList();
        return summary(item, "트렌드 핏", decision,
                primary(formatPercent(metricDecimal(decision, "latestGrowthRate")), "최근 검색 관심도"),
                metrics(metric("하락 키워드", formatPercent(metricDecimal(decision, "decliningKeywordRate"))),
                        metric("개최 시기 관심", formatPercent(metricDecimal(decision, "eventPeriodGap")))),
                chart("LINE", null, chartData));
    }

    private AnalysisItemSummaryResponse demandSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        DemandFitAnalyzer.Result result = demandFitAnalyzer.fromSnapshots(analysis.getFestivalPlan(),
                festivalAnalysisDemandRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()),
                festivalAnalysisAccessibilityRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElse(null));
        InterpretationDecision decision = storedDecisionOrCompute(analysis, item);
        DemandFitResponse.SeasonalDemand seasonal = result.seasonalDemand();
        Integer month = seasonal == null ? null : seasonal.eventMonth();
        Integer monthRank = metricInteger(decision, "eventMonthRank");
        return summary(item, "지역·시기 관광수요 적합성", decision,
                primary(monthRank == null ? "데이터 없음" : monthRank + "위", "개최월 관광수요"),
                metrics(metric("지역 관광수요", formatPercent(metricDecimal(decision, "regionPercentile"))),
                        metric("개최월 관광수요", formatPercent(metricDecimal(decision, "monthPercentile"))),
                        metric("현재 개최 주차", formatRank(metricInteger(decision, "currentWeekRank")))),
                seasonal == null ? null : chart("BAR", month == null ? null : month + "월",
                        seasonal.monthlyAverage() == null ? List.of() : seasonal.monthlyAverage().stream()
                                .filter(value -> value.month() != null && value.visitorAverage() != null)
                                .sorted(Comparator.comparing(DemandFitResponse.MonthlyDemand::month))
                                .map(value -> data(value.month() + "월", value.visitorAverage())).toList()));
    }

    private AnalysisItemSummaryResponse conflictSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        FestivalAnalysisConflict snapshot = festivalAnalysisConflictRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("CONFLICT_RISK snapshot not found: " + analysis.getFestivalAnalysisId()));
        List<ConflictRiskResponse.Event> events = festivalAnalysisConflictEventRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByEventYearAscStartDateAsc(item.getFestivalAnalysisItemId()).stream()
                .map(e -> new ConflictRiskResponse.Event(e.getFestival() == null ? null : e.getFestival().getFestivalId(), e.getEventName(), e.getEventYear(), e.getSido(), e.getSigungu(), e.getRegionRelation(), e.getStartDate(), e.getEndDate(), e.getEventBasis(), e.getConflictType(), e.getOverlapDays(), e.getSameTheme(), e.getVisitorCount())).toList();
        InterpretationDecision decision = storedDecisionOrCompute(analysis, item);
        return summary(item, "일정 중복·혼잡 리스크", decision,
                primary(formatCount(metricInteger(decision, "possibleConflictCount")), "중복 가능 행사"),
                metrics(metric("동일 날짜", formatCount(metricInteger(decision, "directOverlapCount"))),
                        metric("인접 시기", formatCount(metricInteger(decision, "nearbyPeriodCount")))), null);
    }

    private AnalysisItemSummaryResponse weatherSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        FestivalAnalysisWeatherRiskSnapshot snapshot = weatherRiskSnapshotRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("WEATHER_RISK snapshot not found: " + analysis.getFestivalAnalysisId()));
        try {
            WeatherRiskResponse response = objectMapper.readValue(snapshot.getResultJson(), WeatherRiskResponse.class);
            InterpretationDecision decision = storedDecisionOrCompute(analysis, item);
            BigDecimal rain = metricDecimal(decision, "rainOccurrenceRate");
            BigDecimal temperature = metricDecimal(decision, "temperatureOccurrenceRate");
            BigDecimal wind = metricDecimal(decision, "windOccurrenceRate");
            String primaryKey = highestWeatherMetric(rain, temperature, wind);
            String primaryLabel = switch (primaryKey) {
                case "rain" -> "동일 시기 강수 발생률";
                case "temperature" -> switch (String.valueOf(metricValue(decision, "temperatureType"))) {
                    case "HOT" -> "고온 발생률";
                    case "COLD" -> "저온 발생률";
                    default -> "온도 위험";
                };
                default -> "동일 시기 강풍 발생률";
            };
            List<MetricResponse> metrics = new ArrayList<>();
            if (!primaryKey.equals("rain")) metrics.add(metric("강수", formatPercent(rain)));
            if (!primaryKey.equals("temperature")) metrics.add(metric("대표 온도", formatPercent(temperature)));
            if (!primaryKey.equals("wind")) metrics.add(metric("강풍", formatPercent(wind)));
            metrics.add(metric("행사 유형", valueOrData(metricValue(decision, "spaceType"))));
            return summary(item, "날씨 리스크", decision,
                    primary(formatPercent(switch (primaryKey) { case "rain" -> rain; case "temperature" -> temperature; default -> wind; }), primaryLabel),
                    metrics.stream().limit(3).toList(), null);
        } catch (Exception exception) {
            throw new AnalysisExecutionException("WEATHER_RISK snapshot parsing failed: " + analysis.getFestivalAnalysisId(), exception);
        }
    }

    private AnalysisItemSummaryResponse tourismSummary(FestivalAnalysis analysis, FestivalAnalysisItem item) {
        FestivalAnalysisTourismLinkage snapshot = festivalAnalysisTourismLinkageRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("TOURISM_LINKAGE snapshot not found: " + analysis.getFestivalAnalysisId()));
        InterpretationDecision decision = storedDecisionOrCompute(analysis, item);
        return summary(item, "관광 연계 잠재력", decision,
                primary(formatPoi(metricInteger(decision, "totalPoiWithin5km")), "반경 5km 연계 가능 자원"),
                metrics(metric("관광·문화", formatPoi(metricInteger(decision, "tourismCultureWithin3km"))),
                        metric("음식·쇼핑", formatPoi(metricInteger(decision, "foodShoppingWithin3km"))),
                        metric("숙박", formatPoi(metricInteger(decision, "accommodationWithin5km")))), null);
    }

    private AnalysisItemSummaryResponse summary(FestivalAnalysisItem item, String title, InterpretationDecision decision,
                                                PrimaryMetricResponse primary, List<MetricResponse> metrics, ChartResponse chart) {
        return new AnalysisItemSummaryResponse(item.getItemType(), title, decision.status(), decision.statusLevel(), item.getScore(), primary,
                metrics, chart, decision.interpretation() == null ? "데이터 없음" : decision.interpretation().summary());
    }

    private PrimaryMetricResponse primary(String value, String label) { return new PrimaryMetricResponse(value, label); }
    private MetricResponse metric(String label, String value) { return new MetricResponse(label, value); }
    private List<MetricResponse> metrics(MetricResponse... values) { return List.of(values); }
    private ChartResponse chart(String type, String highlightLabel, ChartDataResponse... data) {
        return chart(type, highlightLabel, Arrays.asList(data));
    }
    private ChartResponse chart(String type, String highlightLabel, List<ChartDataResponse> data) {
        return new ChartResponse(type, highlightLabel, data);
    }
    private ChartDataResponse data(String label, BigDecimal value) { return new ChartDataResponse(label, value); }
    private BigDecimal decimal(Long value) { return value == null ? null : BigDecimal.valueOf(value); }
    private Object metricValue(InterpretationDecision decision, String key) { return decision.metrics().stream().filter(m -> key.equals(m.key())).map(InterpretationMetric::value).findFirst().orElse(null); }
    private BigDecimal metricDecimal(InterpretationDecision decision, String key) { Object value = metricValue(decision, key); return value instanceof BigDecimal decimal ? decimal : value instanceof Number number ? BigDecimal.valueOf(number.doubleValue()) : null; }
    private Integer metricInteger(InterpretationDecision decision, String key) { Object value = metricValue(decision, key); return value instanceof Number number ? number.intValue() : null; }
    private String highestWeatherMetric(BigDecimal rain, BigDecimal temperature, BigDecimal wind) { if (temperature != null && (rain == null || temperature.compareTo(rain) >= 0) && (wind == null || temperature.compareTo(wind) >= 0)) return "temperature"; if (rain != null && (wind == null || rain.compareTo(wind) >= 0)) return "rain"; return "wind"; }
    private String valueOrData(Object value) { return value == null ? "데이터 없음" : String.valueOf(value); }
    private String formatPercent(BigDecimal value) { return value == null ? "데이터 없음" : value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%"; }
    private String formatMultiplier(BigDecimal value) { return value == null ? "데이터 없음" : value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "배"; }
    private String formatVisitors(BigDecimal value) { return value == null ? "데이터 없음" : String.format("%,d명", value.setScale(0, RoundingMode.HALF_UP).longValue()); }
    private String formatPoi(Integer value) { return value == null ? "데이터 없음" : value + "개"; }
    private String formatCount(Integer value) { return value == null ? "데이터 없음" : value + "건"; }
    private String formatRank(Integer value) { return value == null ? "데이터 없음" : value + "위"; }

    @Transactional(readOnly = true)
    public TargetVisitorResponse getTargetVisitor(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository
                .findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.TARGET_VISITOR)
                .orElseThrow(() -> new ResourceNotFoundException("TARGET_VISITOR 분석 항목을 찾을 수 없습니다: " + analysisId));
        FestivalAnalysisTargetVisitor snapshot = festivalAnalysisTargetVisitorRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId())
                .orElseThrow(() -> new AnalysisExecutionException("TARGET_VISITOR snapshot not found: " + analysisId));
        List<FestivalAnalysisSimilar> savedSimilar = festivalAnalysisSimilarRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByRankOrderAsc(item.getFestivalAnalysisItemId()).stream()
                .toList();
        List<TargetVisitorResponse.SameFestivalHistory> sameFestivalResponses = savedSimilar.stream()
                .filter(similar -> similar.getComparisonType() == TargetVisitorComparisonType.SAME_FESTIVAL)
                .map(similar -> new TargetVisitorResponse.SameFestivalHistory(0,
                        similar.getFestival() == null ? null : similar.getFestival().getFestivalId(),
                        similar.getFestivalHistory() == null ? null : similar.getFestivalHistory().getFestivalHistoryId(),
                        similar.getFestivalHistory() == null ? similar.getFestivalName()
                                : similar.getFestivalHistory().getFestivalNameRaw(),
                        similar.getYear(), similar.getBudget(), similar.getVisitorCount())).toList();
        List<TargetVisitorResponse.SimilarFestival> similarResponses = savedSimilar.stream()
                .filter(similar -> similar.getComparisonType() != TargetVisitorComparisonType.SAME_FESTIVAL)
                .limit(5)
                .map(similar -> toSimilarFestivalResponse(similar, 0)).toList();
        sameFestivalResponses = reRankSameFestival(sameFestivalResponses);
        similarResponses = reRank(similarResponses);
        return new TargetVisitorResponse(item.getItemType(), item.getScore(), new TargetVisitorResponse.TargetVisitor(
                snapshot.getTargetVisitorCount(), snapshot.getSimilarFestivalCount(), snapshot.getVisitorDataCount(),
                snapshot.getVisitorAverage(), snapshot.getVisitorMedian(), snapshot.getVisitorMin(), snapshot.getVisitorMax(),
                snapshot.getGapRate(), snapshot.getSimilarityThreshold(), sameFestivalResponses, similarResponses),
                recommendationQueryService.findForItem(item), resultInterpretationService.interpretTargetVisitor(
                        analysis.getFestivalPlan(), snapshot.getTargetVisitorCount(), savedSimilar).interpretation());
    }

    private TargetVisitorResponse.SimilarFestival toSimilarFestivalResponse(FestivalAnalysisSimilar similar, int rank) {
        return new TargetVisitorResponse.SimilarFestival(rank,
                similar.getFestival() == null ? null : similar.getFestival().getFestivalId(),
                similar.getFestivalHistory() == null ? null : similar.getFestivalHistory().getFestivalHistoryId(),
                similar.getFestivalName(), similar.getYear(), similar.getBudget(), similar.getVisitorCount(),
                similar.getThemeSimilarity(), similar.getRegionSimilarity(), similar.getPeriodSimilarity(), similar.getSimilarityScore());
    }

    private List<TargetVisitorResponse.SimilarFestival> reRank(List<TargetVisitorResponse.SimilarFestival> festivals) {
        return java.util.stream.IntStream.range(0, festivals.size())
                .mapToObj(index -> {
                    TargetVisitorResponse.SimilarFestival festival = festivals.get(index);
                    return new TargetVisitorResponse.SimilarFestival(index + 1, festival.festivalId(),
                            festival.festivalHistoryId(), festival.festivalName(), festival.year(), festival.budget(),
                            festival.visitorCount(), festival.themeSimilarity(), festival.regionSimilarity(),
                            festival.periodSimilarity(), festival.similarityScore());
                }).toList();
    }

    private List<TargetVisitorResponse.SameFestivalHistory> reRankSameFestival(
            List<TargetVisitorResponse.SameFestivalHistory> festivals) {
        return java.util.stream.IntStream.range(0, festivals.size())
                .mapToObj(index -> {
                    TargetVisitorResponse.SameFestivalHistory festival = festivals.get(index);
                    return new TargetVisitorResponse.SameFestivalHistory(index + 1, festival.festivalId(),
                            festival.festivalHistoryId(), festival.festivalName(), festival.year(),
                            festival.budget(), festival.visitorCount());
                }).toList();
    }

    @Transactional(readOnly = true)
    public DemandFitResponse getDemandFit(Long analysisId) {
        FestivalAnalysis analysis = getAnalysisEntity(analysisId);
        FestivalAnalysisItem item = festivalAnalysisItemRepository.findByFestivalAnalysis_FestivalAnalysisIdAndItemType(analysisId, AnalysisItemType.DEMAND_FIT)
                .orElseThrow(() -> new ResourceNotFoundException("DEMAND_FIT item not found: " + analysisId));
        DemandFitAnalyzer.Result result = demandFitAnalyzer.fromSnapshots(analysis.getFestivalPlan(),
                festivalAnalysisDemandRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()),
                festivalAnalysisAccessibilityRepository.findByFestivalAnalysisItem_FestivalAnalysisItemId(item.getFestivalAnalysisItemId()).orElse(null));
        return new DemandFitResponse(item.getItemType(), item.getScore(), result.regionalDemand(), result.seasonalDemand(), result.accessibility(),
                recommendationQueryService.findForItem(item), resultInterpretationService.interpretDemandFit(
                        analysis.getFestivalPlan(), result).interpretation());
    }

    private String valueOrUnavailable(Object value) {
        return value == null ? "확인되지 않음" : String.valueOf(value);
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
            WeatherRiskResponse response = objectMapper.readValue(snapshot.getResultJson(), WeatherRiskResponse.class);
            return new WeatherRiskResponse(response.itemType(), response.score(), response.station(), response.analysisPeriod(),
                    response.rain(), response.temperature(), response.wind(), response.festivalCondition(),
                    recommendationQueryService.findForItem(item), resultInterpretationService.interpretWeatherRisk(response).interpretation());
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
                snapshot.getSameRegionCount(), events), recommendationQueryService.findForItem(item),
                resultInterpretationService.interpretConflictRisk(snapshot, events).interpretation());
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

    private void saveTargetVisitorSnapshot(FestivalAnalysisItem item, FestivalPlan plan, TargetVisitorAnalyzer.Result result) {
        int visitorDataCount = (int) result.candidates().stream().filter(candidate -> candidate.history().getVisitorCount() != null).count();
        festivalAnalysisTargetVisitorRepository.save(FestivalAnalysisTargetVisitor.builder()
                .festivalAnalysisItem(item).targetVisitorCount(plan.getTargetVisitorCount())
                .similarFestivalCount(result.candidates().size()).visitorDataCount(visitorDataCount)
                .visitorAverage(result.visitorAverage()).visitorMedian(result.visitorMedian())
                .visitorMin(result.visitorMin()).visitorMax(result.visitorMax()).gapRate(result.gapRate())
                .similarityThreshold(result.similarityThreshold()).build());
        List<TargetVisitorAnalyzer.Candidate> sameFestivalHistories = result.candidates().stream()
                .filter(TargetVisitorAnalyzer.Candidate::sameFestival)
                .toList();
        List<TargetVisitorAnalyzer.Candidate> topSimilarFestivals = result.candidates().stream()
                .filter(candidate -> !candidate.sameFestival())
                .limit(5)
                .toList();
        log.info("TARGET_VISITOR snapshot: candidates={}, visitorDataCount={}, average={}, median={}, targetVisitor={}, gapRate={}",
                result.candidates().size(), visitorDataCount, result.visitorAverage(), result.visitorMedian(),
                plan.getTargetVisitorCount(), result.gapRate());
        log.info("TARGET_VISITOR sameFestivalHistories={}", sameFestivalHistories.stream()
                .map(this::formatTargetVisitorCandidate).toList());
        log.info("TARGET_VISITOR topSimilarFestivals={}", topSimilarFestivals.stream()
                .map(this::formatTargetVisitorCandidate).toList());
    }

    private String formatTargetVisitorCandidate(TargetVisitorAnalyzer.Candidate candidate) {
        return (candidate.festival() == null ? candidate.history().getFestivalNameRaw() : candidate.festival().getFestivalName())
                + "(" + candidate.history().getYear() + ") festivalId="
                + (candidate.festival() == null ? null : candidate.festival().getFestivalId())
                + ", budget=" + candidate.history().getBudget()
                + ", visitors=" + candidate.history().getVisitorCount()
                + ", score=" + candidate.similarityScore();
    }

    private void saveTourismLinkageSnapshot(FestivalAnalysisItem item, TourismLinkageAnalyzer.Result result) {
        festivalAnalysisTourismLinkageRepository.save(FestivalAnalysisTourismLinkage.builder()
                .festivalAnalysisItem(item)
                .totalCandidatePoiCount(result.totalCandidatePoiCount())
                .tourismCultureCount(result.tourismCultureCount())
                .foodShoppingCount(result.foodShoppingCount())
                .accommodationCount(result.accommodationCount())
                .tourismCultureWithin3kmCount(result.tourismCultureWithin3kmCount())
                .tourismCultureBetween3And5kmCount(result.tourismCultureBetween3And5kmCount())
                .tourismCultureWithin5kmCount(result.tourismCultureWithin5kmCount())
                .foodShoppingWithin3kmCount(result.foodShoppingWithin3kmCount())
                .foodShoppingBetween3And5kmCount(result.foodShoppingBetween3And5kmCount())
                .foodShoppingWithin5kmCount(result.foodShoppingWithin5kmCount())
                .accommodationWithin3kmCount(result.accommodationWithin3kmCount())
                .accommodationBetween3And5kmCount(result.accommodationBetween3And5kmCount())
                .accommodationWithin5kmCount(result.accommodationWithin5kmCount())
                .resourceDemandBaseYm(result.indicators().resource() == null ? null : result.indicators().resource().baseYm())
                .resourceDemandCode(result.indicators().resource() == null ? null : result.indicators().resource().code())
                .resourceDemandName(result.indicators().resource() == null ? null : result.indicators().resource().name())
                .resourceDemandValue(result.indicators().resource() == null ? null : result.indicators().resource().value())
                .consumptionIntensityBaseYm(result.indicators().consumption() == null ? null : result.indicators().consumption().baseYm())
                .consumptionIntensityCode(result.indicators().consumption() == null ? null : result.indicators().consumption().code())
                .consumptionIntensityName(result.indicators().consumption() == null ? null : result.indicators().consumption().name())
                .consumptionIntensityValue(result.indicators().consumption() == null ? null : result.indicators().consumption().value())
                .stayIntensityBaseYm(result.indicators().stay() == null ? null : result.indicators().stay().baseYm())
                .stayIntensityCode(result.indicators().stay() == null ? null : result.indicators().stay().code())
                .stayIntensityName(result.indicators().stay() == null ? null : result.indicators().stay().name())
                .stayIntensityValue(result.indicators().stay() == null ? null : result.indicators().stay().value())
                .tourismLinkageSummary(result.tourismLinkageSummary())
                .consumptionLinkageSummary(result.consumptionLinkageSummary())
                .stayLinkageSummary(result.stayLinkageSummary())
                .build());
        List<TourismLinkageAnalyzer.Candidate> representativePois = new ArrayList<>();
        representativePois.addAll(result.candidates().stream()
                .filter(candidate -> candidate.poiType() == PoiType.TOURIST_ATTRACTION || candidate.poiType() == PoiType.CULTURAL_FACILITY)
                .limit(5).toList());
        representativePois.addAll(result.candidates().stream()
                .filter(candidate -> candidate.poiType() == PoiType.RESTAURANT || candidate.poiType() == PoiType.SHOPPING)
                .limit(5).toList());
        representativePois.addAll(result.candidates().stream()
                .filter(candidate -> candidate.poiType() == PoiType.ACCOMMODATION)
                .limit(5).toList());
        festivalAnalysisPoiRepository.saveAll(representativePois.stream().map(candidate -> {
            TourApiClient.Poi poi = candidate.poi();
            return FestivalAnalysisPoi.builder().festivalAnalysisItem(item).contentId(poi.contentId())
                    .contentTypeId(poi.contentTypeId()).poiName(poi.title()).poiType(candidate.poiType())
                    .distanceM(candidate.distanceM()).latitude(poi.latitude()).longitude(poi.longitude())
                    .address(poi.address()).imageUrl(poi.imageUrl()).linkageType(candidate.linkageType())
                    .distanceRange(candidate.distanceRange()).build();
        }).toList());
    }

    private void saveDemandSnapshots(FestivalAnalysisItem item, FestivalPlan plan, DemandFitAnalyzer.Result result) {
        List<FestivalAnalysisDemand> rows = new ArrayList<>();
        result.regionalYears().forEach(row -> rows.add(FestivalAnalysisDemand.builder().festivalAnalysisItem(item).demandType(DemandType.REGIONAL).regionCode(row.code()).sido(row.sido()).sigungu(row.name()).statYear(row.year()).visitorCount(row.value()).build()));
        result.dailyRecords().forEach(row -> rows.add(FestivalAnalysisDemand.builder().festivalAnalysisItem(item).demandType(DemandType.SEASONAL).regionCode(row.regionCode()).sido(plan.getSido()).sigungu(row.regionName()).statYear(row.date().getYear()).statMonth(row.date().getMonthValue()).statDay(row.date().getDayOfMonth()).visitorCount(row.visitorCount()).build()));
            festivalAnalysisDemandRepository.saveAll(rows);
        DemandFitResponse.Bus bus = result.accessibility().bus(); DemandFitResponse.Rail rail = result.accessibility().rail();
        DemandFitResponse.Parking parking = result.accessibility().parking();
        festivalAnalysisAccessibilityRepository.save(FestivalAnalysisAccessibility.builder().festivalAnalysisItem(item).nearestBusStopName(bus.nearestStopName()).nearestBusStopDistanceM(bus.nearestStopDistanceM()).busStopCount500m(bus.stopCount500m()).busStopCount1km(bus.stopCount1km()).busRouteCount(bus.routeCount()).railAvailable(rail.available()).nearestStationName(rail.nearestStationName()).nearestStationDistanceM(rail.nearestStationDistanceM()).parkingCount(parking.parkingCount()).parkingCapacity(parking.parkingCapacity()).build());
    }
    private FestivalAnalysis getAnalysisEntity(Long analysisId) {
        return festivalAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("분석 결과를 찾을 수 없습니다: " + analysisId));
    }

    public void verifyAnalysisOwner(Long analysisId) {
        FestivalAnalysis analysis = festivalAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long userId)) throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        if (!userId.equals(analysis.getFestivalPlan().getUser().getUserId())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
    }

    private void verifyOwner(Long ownerId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long userId)) throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        if (!userId.equals(ownerId)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
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
                keywordTrends, aroundEvent, recommendationQueryService.findForItem(item),
                resultInterpretationService.interpretTrendFit(analysis, yearlyRows, monthlyRows).interpretation());
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
                .scoreGrade(analysisScoreService.grade(totalScore))
                .analysisStatus(status)
                .startedAt(analysis.getStartedAt())
                .completedAt(completedAt)
                .build());
    }

    private BigDecimal calculateMedian(List<Long> sortedValues) {
        if (sortedValues.isEmpty()) throw new AnalysisExecutionException("유사 축제 방문객 데이터가 없습니다.");
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) return BigDecimal.valueOf(sortedValues.get(middle));
        return BigDecimal.valueOf(sortedValues.get(middle - 1))
                .add(BigDecimal.valueOf(sortedValues.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
