package likelion.festivalscope.analysis.recommendation.dto;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysis;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.FestivalAnalysisSimilar;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTargetVisitor;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTrendKeyword;
import likelion.festivalscope.analysis.entity.FestivalAnalysisAccessibility;
import likelion.festivalscope.analysis.entity.FestivalAnalysisConflict;
import likelion.festivalscope.analysis.entity.FestivalAnalysisDemand;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTourismLinkage;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.plan.entity.FestivalPlan;

import java.util.List;
import java.util.Map;

public record RecommendationContext(
        FestivalAnalysis analysis,
        FestivalPlan plan,
        Map<AnalysisItemType, FestivalAnalysisItem> items,
        TargetVisitorData targetVisitor,
        List<FestivalAnalysisTrendKeyword> trendKeywords,
        DemandData demand,
        FestivalAnalysisConflict conflict,
        WeatherRiskResponse weatherRisk,
        FestivalAnalysisTourismLinkage tourismLinkage
) {
    public FestivalAnalysisItem item(AnalysisItemType itemType) {
        return items.get(itemType);
    }

    public record TargetVisitorData(
            FestivalAnalysisTargetVisitor snapshot,
            List<FestivalAnalysisSimilar> similarFestivals
    ) {
    }

    public record DemandData(
            List<FestivalAnalysisDemand> demandRows,
            FestivalAnalysisAccessibility accessibility
    ) {
    }
}
