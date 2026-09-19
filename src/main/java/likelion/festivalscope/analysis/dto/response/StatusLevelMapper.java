package likelion.festivalscope.analysis.dto.response;

import likelion.festivalscope.analysis.entity.StatusLevel;

import java.util.List;

public final class StatusLevelMapper {
    private StatusLevelMapper() {
    }

    public static StatusLevel from(String status, List<InterpretationMetric> metrics) {
        if (status == null || "데이터 부족".equals(status)) return StatusLevel.UNKNOWN;
        return switch (status) {
            case "과다 설정", "보수적 설정", "관심 급감", "수요 취약", "중복 리스크 높음", "기상 리스크 높음", "취약" -> StatusLevel.NEGATIVE;
            case "다소 높음", "다소 낮음", "관심 하락", "중복 주의", "기상 주의" -> StatusLevel.WARNING;
            case "적정", "관심 급상승", "관심 상승", "수요 우수", "시기 강점", "지역 강점", "중복 리스크 낮음", "기상 리스크 낮음", "연계 잠재력 높음" -> StatusLevel.POSITIVE;
            case "관심 유지", "보통", "기상 영향 제한" -> StatusLevel.NEUTRAL;
            default -> StatusLevel.UNKNOWN;
        };
    }
}
