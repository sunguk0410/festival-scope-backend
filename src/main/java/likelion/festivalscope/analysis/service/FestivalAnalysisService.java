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
import likelion.festivalscope.analysis.dto.response.TargetVisitorResponse;
import likelion.festivalscope.analysis.dto.response.TrendFitResponse;
import likelion.festivalscope.analysis.dto.response.ResultInterpretation;
import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.plan.entity.*;
import likelion.festivalscope.festival.entity.*;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.AnalysisStatus;
import likelion.festivalscope.external.tourism.TourApiClient;
import likelion.festivalscope.analysis.repository.*;
import likelion.festivalscope.analysis.recommendation.service.RecommendationService;
import likelion.festivalscope.analysis.dto.response.RecommendationResponse;
import likelion.festivalscope.analysis.recommendation.repository.FestivalAnalysisRecommendationRepository;
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
    private final FestivalAnalysisRecommendationRepository festivalAnalysisRecommendationRepository;
    private final ResultInterpretationService resultInterpretationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

            replaceAnalysisStatus(analysis, null, AnalysisStatus.COMPLETED, LocalDateTime.now());
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
                recommendationsForItem(item), resultInterpretationService.interpretTourismLinkage(snapshot));
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

    private RecommendationResponse toRecommendationResponse(
            FestivalAnalysisRecommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getRecommendationId(),
                recommendation.getRecommendationType(),
                recommendation.getPriority(),
                recommendation.getTitle(),
                recommendation.getContent(),
                recommendation.getDisplayOrder());
    }

    private List<RecommendationResponse> recommendationsForItem(FestivalAnalysisItem item) {
        return festivalAnalysisRecommendationRepository
                .findAllByFestivalAnalysis_FestivalAnalysisIdOrderByDisplayOrderAscRecommendationIdAsc(
                        item.getFestivalAnalysis().getFestivalAnalysisId())
                .stream()
                .filter(recommendation -> recommendation.getFestivalAnalysisItem() != null
                        && recommendation.getFestivalAnalysisItem().getFestivalAnalysisItemId()
                        .equals(item.getFestivalAnalysisItemId()))
                .map(this::toRecommendationResponse)
                .toList();
    }

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
                recommendationsForItem(item), resultInterpretationService.interpretTargetVisitor(
                        analysis.getFestivalPlan(), snapshot.getTargetVisitorCount(), savedSimilar));
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
                recommendationsForItem(item), resultInterpretationService.interpretDemandFit(
                        analysis.getFestivalPlan(), result));
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
                    recommendationsForItem(item), resultInterpretationService.interpretWeatherRisk(response));
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
                snapshot.getSameRegionCount(), events), recommendationsForItem(item),
                resultInterpretationService.interpretConflictRisk(snapshot, events));
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
                keywordTrends, aroundEvent, recommendationsForItem(item),
                resultInterpretationService.interpretTrendFit(analysis, yearlyRows, monthlyRows));
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
        if (sortedValues.isEmpty()) throw new AnalysisExecutionException("유사 축제 방문객 데이터가 없습니다.");
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) return BigDecimal.valueOf(sortedValues.get(middle));
        return BigDecimal.valueOf(sortedValues.get(middle - 1))
                .add(BigDecimal.valueOf(sortedValues.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
