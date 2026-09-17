package likelion.festivalscope.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "User not found."),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_001", "Festival plan not found."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "Analysis not found."),
    ANALYSIS_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_002", "Analysis item not found."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "Invalid request."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_001", "Validation failed."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "COMMON_002", "Malformed JSON."),
    ANALYSIS_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_003", "Analysis execution failed."),
    TOURISM_VISITOR_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DEMAND_001", "Tourism visitor API failed."),
    BUS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DEMAND_002", "Bus API failed."),
    VENUE_COORDINATE_REQUIRED(HttpStatus.BAD_REQUEST, "PLAN_002", "Venue coordinates are required."),
    AUTH_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "AUTH_001", "Email is already registered."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "Email or password is invalid."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_003", "Invalid or expired token."),
    AUTH_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_004", "Refresh token is not found."),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "Refresh token has expired."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_006", "Authentication is required."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_007", "You do not own this resource."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "Internal server error.");
    private final HttpStatus status; private final String code; private final String message;
    ErrorCode(HttpStatus status, String code, String message) { this.status=status; this.code=code; this.message=message; }
}
