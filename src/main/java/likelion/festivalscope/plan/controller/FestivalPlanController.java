package likelion.festivalscope.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import likelion.festivalscope.global.response.ErrorResponse;
import likelion.festivalscope.plan.dto.request.FestivalPlanCreateRequest;
import likelion.festivalscope.plan.dto.response.FestivalPlanCreateResponse;
import likelion.festivalscope.plan.service.FestivalPlanService;
import likelion.festivalscope.trend.dto.TrendKeywordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/festival-plans")
@Tag(name = "Festival Plan", description = "축제 기획안 등록 및 관리 API")
public class FestivalPlanController {
    private final FestivalPlanService festivalPlanService;

    @Operation(summary = "축제 기획안 등록", description = "축제 기획안의 기본 정보와 프로그램 정보를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "기획안 등록 성공", content = @Content(schema = @Schema(implementation = likelion.festivalscope.global.response.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FestivalPlanCreateResponse create(@Valid @RequestBody FestivalPlanCreateRequest request) {
        return festivalPlanService.create(request);
    }

    @Operation(summary = "[개발용/테스트용] 트렌드 검색 키워드 추출 검증", description = "키워드 추출 품질 검증을 위한 테스트용 API입니다. 운영 분석에서는 이 API를 별도로 호출하지 않으며, POST /api/festival-plans/{planId}/analyses 내부에서 동일한 TrendKeywordService가 자동 실행됩니다.")
    @PostMapping("/{planId}/trend-keywords")
    public TrendKeywordResponse generateTrendKeywords(@PathVariable Long planId) {
        return festivalPlanService.generateTrendKeywords(planId);
    }
}
