package likelion.festivalscope.analysis.dto.response;

import java.util.List;

public record InterpretationDecision(
        String status,
        ResultInterpretation interpretation,
        List<InterpretationMetric> metrics
) {
    public InterpretationDecision {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
