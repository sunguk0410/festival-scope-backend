package likelion.festivalscope.analysis.orchestration;

public enum AnalysisStep {
    SIMILAR_FESTIVAL("유사 축제 실적 조회"),
    TREND_FIT("주제 키워드 관심도 추이 분석"),
    DEMAND_FIT("지역 월별 관광수요·접근성 집계"),
    COMPETITION_RISK("인접 권역 행사 이력 대조"),
    WEATHER_RISK("과거 동일 시기 기상 통계 집계"),
    TOURISM_LINKAGE("행사장 주변 POI 분석"),
    FINALIZING("분석 결과 종합 중");

    private final String label;

    AnalysisStep(String label) { this.label = label; }

    public String getLabel() { return label; }
}
