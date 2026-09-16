package likelion.festivalscope.analysis.analyzer;

import java.math.BigDecimal;

public interface TrendScoreCalculator {
    BigDecimal calculate(TrendAnalysisResult result);
}
