package likelion.festivalscope.global.exception;

import jakarta.validation.ConstraintViolationException;
import likelion.festivalscope.global.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse<Void>> handleBusiness(BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Business exception: method={}, uri={}, errorCode={}, message={}",
                request.getMethod(), request.getRequestURI(), errorCode.getCode(), exception.getMessage());
        return response(errorCode, exception.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse<Void>> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        ErrorCode errorCode = resolveNotFoundCode(exception.getMessage());
        log.warn("Resource not found: method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return response(errorCode, errorCode.getMessage(), null);
    }

    @ExceptionHandler(AnalysisExecutionException.class)
    public ResponseEntity<ErrorResponse<Void>> handleAnalysis(AnalysisExecutionException exception, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.ANALYSIS_EXECUTION_FAILED;
        log.error("Analysis execution failed: method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
        return response(errorCode, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getMessage(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return response(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getMessage(), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse<Void>> handleUnreadable(HttpMessageNotReadableException exception) {
        return response(ErrorCode.MALFORMED_JSON, ErrorCode.MALFORMED_JSON.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception: method={}, uri={}, query={}, exceptionType={}, message={}",
                request.getMethod(), request.getRequestURI(), request.getQueryString(),
                exception.getClass().getName(), exception.getMessage(), exception);
        return response(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), null);
    }

    private ErrorCode resolveNotFoundCode(String message) {
        if (message != null && message.contains("userId")) return ErrorCode.USER_NOT_FOUND;
        if (message != null && message.contains("planId")) return ErrorCode.PLAN_NOT_FOUND;
        if (message != null && message.contains("항목")) return ErrorCode.ANALYSIS_ITEM_NOT_FOUND;
        return ErrorCode.ANALYSIS_NOT_FOUND;
    }
    private <T> ResponseEntity<ErrorResponse<T>> response(ErrorCode errorCode, String message, T data) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getCode(), message, data));
    }
}
