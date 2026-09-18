package likelion.festivalscope.analysis.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysis;
import likelion.festivalscope.analysis.entity.FestivalAnalysisConflict;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.FestivalAnalysisSimilar;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTargetVisitor;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTrendKeyword;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTourismLinkage;
import likelion.festivalscope.analysis.entity.PeriodType;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.repository.FestivalAnalysisItemRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisSimilarRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisTargetVisitorRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisTrendKeywordRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisAccessibilityRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisDemandRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisConflictRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisWeatherRiskSnapshotRepository;
import likelion.festivalscope.analysis.repository.FestivalAnalysisTourismLinkageRepository;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationContextFactory {
    private final FestivalAnalysisItemRepository festivalAnalysisItemRepository;
    private final FestivalAnalysisTargetVisitorRepository festivalAnalysisTargetVisitorRepository;
    private final FestivalAnalysisSimilarRepository festivalAnalysisSimilarRepository;
    private final FestivalAnalysisTrendKeywordRepository festivalAnalysisTrendKeywordRepository;
    private final FestivalAnalysisDemandRepository festivalAnalysisDemandRepository;
    private final FestivalAnalysisAccessibilityRepository festivalAnalysisAccessibilityRepository;
    private final FestivalAnalysisConflictRepository festivalAnalysisConflictRepository;
    private final FestivalAnalysisWeatherRiskSnapshotRepository festivalAnalysisWeatherRiskSnapshotRepository;
    private final FestivalAnalysisTourismLinkageRepository festivalAnalysisTourismLinkageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecommendationContext create(FestivalAnalysis analysis) {
        List<FestivalAnalysisItem> itemList = festivalAnalysisItemRepository
                .findAllByFestivalAnalysis_FestivalAnalysisIdOrderByFestivalAnalysisItemIdAsc(
                        analysis.getFestivalAnalysisId());
        Map<AnalysisItemType, FestivalAnalysisItem> items = new EnumMap<>(AnalysisItemType.class);
        itemList.forEach(item -> items.put(item.getItemType(), item));

        FestivalAnalysisItem targetItem = items.get(AnalysisItemType.TARGET_VISITOR);
        FestivalAnalysisTargetVisitor targetSnapshot = targetItem == null ? null
                : festivalAnalysisTargetVisitorRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(targetItem.getFestivalAnalysisItemId())
                .orElse(null);
        List<FestivalAnalysisSimilar> similarFestivals = targetItem == null ? List.of()
                : festivalAnalysisSimilarRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdOrderByRankOrderAsc(
                        targetItem.getFestivalAnalysisItemId());

        FestivalAnalysisItem trendItem = items.get(AnalysisItemType.TREND_FIT);
        List<FestivalAnalysisTrendKeyword> trendKeywords = trendItem == null ? List.of()
                : trendRows(trendItem);

        FestivalAnalysisItem demandItem = items.get(AnalysisItemType.DEMAND_FIT);
        List<likelion.festivalscope.analysis.entity.FestivalAnalysisDemand> demandRows = demandItem == null
                ? List.of()
                : festivalAnalysisDemandRepository.findAllByFestivalAnalysisItem_FestivalAnalysisItemId(
                demandItem.getFestivalAnalysisItemId());
        likelion.festivalscope.analysis.entity.FestivalAnalysisAccessibility accessibility = demandItem == null
                ? null
                : festivalAnalysisAccessibilityRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(demandItem.getFestivalAnalysisItemId())
                .orElse(null);

        FestivalAnalysisItem conflictItem = items.get(AnalysisItemType.CONFLICT_RISK);
        FestivalAnalysisConflict conflict = conflictItem == null ? null
                : festivalAnalysisConflictRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(conflictItem.getFestivalAnalysisItemId())
                .orElse(null);

        FestivalAnalysisItem weatherItem = items.get(AnalysisItemType.WEATHER_RISK);
        WeatherRiskResponse weatherRisk = weatherItem == null ? null : weatherRiskSnapshot(weatherItem);

        FestivalAnalysisItem tourismItem = items.get(AnalysisItemType.TOURISM_LINKAGE);
        FestivalAnalysisTourismLinkage tourismLinkage = tourismItem == null ? null
                : festivalAnalysisTourismLinkageRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(tourismItem.getFestivalAnalysisItemId())
                .orElse(null);

        return new RecommendationContext(
                analysis,
                analysis.getFestivalPlan(),
                items,
                new RecommendationContext.TargetVisitorData(targetSnapshot, similarFestivals),
                trendKeywords,
                new RecommendationContext.DemandData(demandRows, accessibility),
                conflict,
                weatherRisk,
                tourismLinkage);
    }

    private List<FestivalAnalysisTrendKeyword> trendRows(FestivalAnalysisItem trendItem) {
        List<FestivalAnalysisTrendKeyword> rows = new java.util.ArrayList<>();
        rows.addAll(festivalAnalysisTrendKeywordRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
                        trendItem.getFestivalAnalysisItemId(), PeriodType.YEARLY));
        rows.addAll(festivalAnalysisTrendKeywordRepository
                .findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
                        trendItem.getFestivalAnalysisItemId(), PeriodType.MONTHLY));
        return rows;
    }

    private WeatherRiskResponse weatherRiskSnapshot(FestivalAnalysisItem weatherItem) {
        return festivalAnalysisWeatherRiskSnapshotRepository
                .findByFestivalAnalysisItem_FestivalAnalysisItemId(weatherItem.getFestivalAnalysisItemId())
                .map(snapshot -> readWeatherRisk(snapshot.getResultJson()))
                .orElse(null);
    }

    private WeatherRiskResponse readWeatherRisk(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) return null;
        try {
            return objectMapper.readValue(resultJson, WeatherRiskResponse.class);
        } catch (Exception exception) {
            log.warn("WEATHER_RISK recommendation snapshot parsing failed", exception);
            return null;
        }
    }
}
