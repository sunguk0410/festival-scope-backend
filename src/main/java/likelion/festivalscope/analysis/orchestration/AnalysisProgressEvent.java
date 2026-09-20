package likelion.festivalscope.analysis.orchestration;

public record AnalysisProgressEvent(String step, AnalysisStepStatus status, String message) {}
