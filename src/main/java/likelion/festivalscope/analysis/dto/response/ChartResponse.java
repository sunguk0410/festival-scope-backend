package likelion.festivalscope.analysis.dto.response;

import java.util.List;

public record ChartResponse(String type, String highlightLabel, List<ChartDataResponse> data) {
}
