package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.AnalysisItemType;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisItemSummaryResponse(
        AnalysisItemType itemType,
        String title,
        String status,
        BigDecimal score,
        PrimaryMetricResponse primaryMetric,
        List<MetricResponse> metrics,
        ChartResponse chart,
        String summary
) {
}
