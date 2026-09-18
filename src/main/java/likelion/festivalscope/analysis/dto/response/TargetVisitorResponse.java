package likelion.festivalscope.analysis.dto.response;
import likelion.festivalscope.analysis.entity.AnalysisItemType;
import java.math.BigDecimal;
import java.util.List;
public record TargetVisitorResponse(AnalysisItemType itemType, BigDecimal score, TargetVisitor targetVisitor,
                                    List<RecommendationResponse> recommendations) {
 public record TargetVisitor(Long targetVisitorCount,Integer similarFestivalCount,Integer visitorDataCount,BigDecimal visitorAverage,BigDecimal visitorMedian,Long visitorMin,Long visitorMax,BigDecimal gapRate,BigDecimal similarityThreshold,List<SameFestivalHistory> sameFestivalHistories,List<SimilarFestival> topSimilarFestivals) {}
 public record SameFestivalHistory(Integer rank,Long festivalId,Long festivalHistoryId,String festivalName,Integer year,BigDecimal budget,Long visitorCount) {}
 public record SimilarFestival(Integer rank,Long festivalId,Long festivalHistoryId,String festivalName,Integer year,BigDecimal budget,Long visitorCount,BigDecimal themeSimilarity,BigDecimal regionSimilarity,BigDecimal periodSimilarity,BigDecimal similarityScore) {}
}
