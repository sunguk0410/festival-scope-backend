package likelion.festivalscope.global.exception;

public class AnalysisExecutionException extends RuntimeException {
    public AnalysisExecutionException(String message) {
        super(message);
    }

    public AnalysisExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
