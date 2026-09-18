package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.FestivalAnalysis;
import likelion.festivalscope.analysis.entity.AnalysisStatus;
import likelion.festivalscope.analysis.dto.response.AnalysisListResponse.AnalysisListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FestivalAnalysisRepository extends JpaRepository<FestivalAnalysis, Long> {
    List<FestivalAnalysis> findAllByFestivalPlan_FestivalPlanIdOrderByCreatedAtDesc(Long festivalPlanId);

    @Query(value = """
            select fa.festivalAnalysisId as analysisId,
                   fp.festivalName as festivalName,
                   fp.sido as sido,
                   fp.sigungu as sigungu,
                   fp.createdAt as inputDate,
                   fa.totalScore as overallScore,
                   fp.startDate as festivalStartDate,
                   fp.endDate as festivalEndDate,
                   (select count(recommendation.recommendationId)
                      from FestivalAnalysisRecommendation recommendation
                     where recommendation.festivalAnalysis.festivalAnalysisId = fa.festivalAnalysisId)
                   as recommendationCount
            from FestivalAnalysis fa
            join fa.festivalPlan fp
            where fp.user.userId = :userId
              and fa.analysisStatus = :status
            order by fp.createdAt desc, fa.festivalAnalysisId desc
            """,
            countQuery = """
            select count(fa.festivalAnalysisId)
            from FestivalAnalysis fa
            join fa.festivalPlan fp
            where fp.user.userId = :userId
              and fa.analysisStatus = :status
            """)
    Page<AnalysisListProjection> findCompletedAnalysisList(
            @Param("userId") Long userId,
            @Param("status") AnalysisStatus status,
            Pageable pageable);
}
