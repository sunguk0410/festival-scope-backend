package likelion.festivalscope.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다."),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_001", "존재하지 않는 기획안입니다."),
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "존재하지 않는 분석 결과입니다."),
    ANALYSIS_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_002", "존재하지 않는 분석 항목입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "요청 값이 올바르지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_001", "요청 값이 올바르지 않습니다."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "COMMON_002", "요청 JSON 형식이 올바르지 않습니다."),
    ANALYSIS_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_003", "분석 실행에 실패했습니다."),
    TOURISM_VISITOR_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DEMAND_001", "관광객 방문자 API 호출에 실패했습니다."),
    BUS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DEMAND_002", "버스 접근성 API 호출에 실패했습니다."),
    VENUE_COORDINATE_REQUIRED(HttpStatus.BAD_REQUEST, "PLAN_002", "행사장의 위도와 경도가 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
