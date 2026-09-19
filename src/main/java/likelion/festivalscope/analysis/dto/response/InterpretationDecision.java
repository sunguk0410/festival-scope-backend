package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.StatusLevel;

import java.util.List;

public record InterpretationDecision(
        String status,
        StatusLevel statusLevel,
        ResultInterpretation interpretation,
        List<InterpretationMetric> metrics
) {
    public InterpretationDecision(String status, ResultInterpretation interpretation,
                                  List<InterpretationMetric> metrics) {
        this(status, StatusLevelMapper.from(status, metrics), interpretation, metrics);
    }

    public InterpretationDecision {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        statusLevel = statusLevel == null ? StatusLevelMapper.from(status, metrics) : statusLevel;
    }
}
