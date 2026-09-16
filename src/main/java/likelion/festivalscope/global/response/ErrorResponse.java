package likelion.festivalscope.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "공통 실패 응답")
public class ErrorResponse<T> {
    @Schema(description = "요청 성공 여부", example = "false")
    private final boolean success = false;
    @Schema(description = "프론트에서 분기할 오류 코드", example = "PLAN_001")
    private final String errorCode;
    @Schema(description = "오류 메시지", example = "존재하지 않는 기획안입니다.")
    private final String message;
    @Schema(description = "Validation 오류 필드 등 부가 데이터", nullable = true)
    private final T data;

    private ErrorResponse(String errorCode, String message, T data) {
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    public static <T> ErrorResponse<T> of(String errorCode, String message, T data) {
        return new ErrorResponse<>(errorCode, message, data);
    }
}