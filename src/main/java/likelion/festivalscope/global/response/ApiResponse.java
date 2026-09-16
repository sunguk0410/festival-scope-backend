package likelion.festivalscope.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "공통 성공 응답")
public class ApiResponse<T> {
    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;
    @Schema(description = "응답 메시지", nullable = true, example = "기획안이 등록되었습니다.")
    private final String message;
    @Schema(description = "실제 응답 데이터")
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) { return new ApiResponse<>(true, null, data); }
    public static <T> ApiResponse<T> success(String message, T data) { return new ApiResponse<>(true, message, data); }
    public static <T> ApiResponse<T> fail(String message) { return new ApiResponse<>(false, message, null); }
}