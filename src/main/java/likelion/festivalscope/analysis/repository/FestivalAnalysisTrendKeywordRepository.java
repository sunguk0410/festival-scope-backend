package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysisTrendKeyword;
import likelion.festivalscope.analysis.entity.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalAnalysisTrendKeywordRepository extends JpaRepository<FestivalAnalysisTrendKeyword, Long> {
    List<FestivalAnalysisTrendKeyword> findAllByFestivalAnalysisItem_FestivalAnalysisItemIdAndPeriodTypeOrderByKeywordAscPeriodYearAscPeriodMonthAsc(
            Long itemId, PeriodType periodType);
}
