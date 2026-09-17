package likelion.festivalscope.analysis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import likelion.festivalscope.analysis.dto.response.FestivalAnalysisResponse;
import likelion.festivalscope.analysis.dto.response.TargetVisitorResponse;
import likelion.festivalscope.analysis.dto.response.TrendFitResponse;
import likelion.festivalscope.analysis.dto.response.DemandFitResponse;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.analysis.dto.response.ConflictRiskResponse;
import likelion.festivalscope.analysis.service.FestivalAnalysisService;
import likelion.festivalscope.global.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Festival Analysis", description = "축제 기획안 분석 실행 및 결과 조회 API")
public class FestivalAnalysisController {
    private final FestivalAnalysisService festivalAnalysisService;

    @Operation(summary = "축제 기획안 분석 실행", description = "저장된 축제 기획안을 기반으로 분석을 실행합니다. 현재 TARGET_VISITOR와 TREND_FIT 분석이 구현되어 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "분석 실행 성공", content = @Content(schema = @Schema(implementation = likelion.festivalscope.global.response.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "기획안을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "분석 실행 또는 외부 데이터 연동 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/festival-plans/{planId}/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public Long execute(@Parameter(description = "분석할 FestivalPlan ID", example = "1") @PathVariable Long planId) {
        return festivalAnalysisService.execute(planId);
    }

    @Operation(summary = "축제 분석 결과 요약 조회", description = "분석 결과의 종합 점수, 분석 상태 및 분석 항목 목록을 조회합니다. 각 항목의 상세 데이터는 별도 상세 API에서 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 결과 조회 성공", content = @Content(schema = @Schema(implementation = likelion.festivalscope.global.response.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/analyses/{analysisId}")
    public FestivalAnalysisResponse getAnalysis(@Parameter(description = "조회할 FestivalAnalysis ID", example = "1") @PathVariable Long analysisId) {
        festivalAnalysisService.verifyAnalysisOwner(analysisId);
        return festivalAnalysisService.getAnalysis(analysisId);
    }

    @Operation(summary = "목표 방문객 타당성 상세 조회", description = "설정된 목표 방문객 수와 유사 축제의 실제 방문객 규모를 비교한 상세 분석 결과를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TARGET_VISITOR 상세 조회 성공", content = @Content(schema = @Schema(implementation = likelion.festivalscope.global.response.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 또는 분석 항목을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/analyses/{analysisId}/items/TARGET_VISITOR")
    public TargetVisitorResponse getTargetVisitor(@Parameter(description = "조회할 FestivalAnalysis ID", example = "1") @PathVariable Long analysisId) {
        festivalAnalysisService.verifyAnalysisOwner(analysisId);
        return festivalAnalysisService.getTargetVisitor(analysisId);
    }

    @Operation(summary = "트렌드 핏 상세 조회", description = "네이버 데이터랩 검색 관심도를 기반으로 축제 콘텐츠 관련 키워드의 최근 관심 흐름을 조회합니다. 현재 TREND_FIT 점수 정책은 미확정이므로 score는 null일 수 있으며 null은 0점이 아닙니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TREND_FIT 상세 조회 성공", content = @Content(schema = @Schema(implementation = likelion.festivalscope.global.response.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 또는 분석 항목을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Naver DataLab 연동 또는 분석 스냅샷 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/analyses/{analysisId}/items/TREND_FIT")
    public TrendFitResponse getTrendFit(@Parameter(description = "조회할 FestivalAnalysis ID", example = "1") @PathVariable Long analysisId) {
        festivalAnalysisService.verifyAnalysisOwner(analysisId);
        return festivalAnalysisService.getTrendFit(analysisId);
    }
    @Operation(summary = "지역·시기 관광수요 적합성 상세 조회", description = "동일 광역권 유사 행정지역 대비 관광수요, 최근 3개년 월별 및 개최월 주차별 관광수요, 버스·도시철도 접근성을 조회합니다. score는 정책 미정으로 null일 수 있으며 0점을 의미하지 않습니다. weeklyDemand의 referenceOnly=true는 5주차 실제 일수가 3일 미만이라 추천 주차 선정에서 제외된 참고 데이터입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DEMAND_FIT 상세 조회 성공", content = @Content(schema = @Schema(implementation = likelion.festivalscope.global.response.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 또는 분석 항목을 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "관광수요·접근성 외부 API 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/analyses/{analysisId}/items/DEMAND_FIT")
    public DemandFitResponse getDemandFit(@Parameter(description = "조회할 FestivalAnalysis ID", example = "1") @PathVariable Long analysisId) {
        festivalAnalysisService.verifyAnalysisOwner(analysisId);
        return festivalAnalysisService.getDemandFit(analysisId);
    }

    @Operation(summary = "WEATHER_RISK 상세 조회")
    @GetMapping("/api/analyses/{analysisId}/items/WEATHER_RISK")
    public WeatherRiskResponse getWeatherRisk(@PathVariable Long analysisId) {
        festivalAnalysisService.verifyAnalysisOwner(analysisId);
        return festivalAnalysisService.getWeatherRisk(analysisId);
    }

    @Operation(summary = "CONFLICT_RISK 상세 조회")
    @GetMapping("/api/analyses/{analysisId}/items/CONFLICT_RISK")
    public ConflictRiskResponse getConflictRisk(@PathVariable Long analysisId) {
        festivalAnalysisService.verifyAnalysisOwner(analysisId);
        return festivalAnalysisService.getConflictRisk(analysisId);
    }

    @Operation(summary = "WEATHER_RISK API 테스트", description = "분석 결과나 snapshot을 저장하지 않고 축제 계획의 좌표와 기간으로 날씨 분석만 실행합니다.")
    @GetMapping("/api/festival-plans/{planId}/weather-risk/test")
    public WeatherRiskResponse testWeatherRisk(@PathVariable Long planId) {
        return festivalAnalysisService.testWeatherRisk(planId);
    }
}
