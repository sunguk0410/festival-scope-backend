package likelion.festivalscope.analysis.repository;

import likelion.festivalscope.analysis.entity.RegionalVisitorStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegionalVisitorStatRepository extends JpaRepository<RegionalVisitorStat, Long> {
    long countByBaseYmdBetween(LocalDate start, LocalDate end);

    @Query("SELECT MIN(r.baseYmd) FROM RegionalVisitorStat r WHERE r.baseYmd BETWEEN :from AND :to")
    LocalDate findMinDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT MAX(r.baseYmd) FROM RegionalVisitorStat r WHERE r.baseYmd BETWEEN :from AND :to")
    LocalDate findMaxDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(DISTINCT r.baseYmd) FROM RegionalVisitorStat r WHERE r.baseYmd BETWEEN :from AND :to")
    long countDistinctDates(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(r) FROM RegionalVisitorStat r WHERE r.baseYmd BETWEEN :from AND :to AND r.sidoName IS NULL")
    long countWithoutSido(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteAllByBaseYmdBetween(LocalDate from, LocalDate to);

    @Query("SELECT CONCAT(r.baseYmd, '|', r.signguCode) FROM RegionalVisitorStat r "
            + "WHERE r.baseYmd BETWEEN :from AND :to")
    List<String> findKeysByBaseYmdBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT r.signguCode AS signguCode,
                   r.signguName AS signguName,
                   r.sidoName AS sidoName,
                   FUNCTION('YEAR', r.baseYmd) AS statYear,
                   SUM(r.visitorCount) AS visitorCount
            FROM RegionalVisitorStat r
            WHERE r.baseYmd BETWEEN :from AND :to
            GROUP BY r.signguCode, r.signguName, r.sidoName, FUNCTION('YEAR', r.baseYmd)
            """)
    List<RegionalYearProjection> findRegionalYearAggregates(@Param("from") LocalDate from,
                                                             @Param("to") LocalDate to);

    @Query("SELECT DISTINCT r.signguCode FROM RegionalVisitorStat r "
            + "WHERE r.signguName = :signguName AND r.baseYmd BETWEEN :from AND :to")
    List<String> findDistinctSignguCodesBySignguNameAndBaseYmdBetween(
            @Param("signguName") String signguName,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT r.baseYmd AS baseYmd,
                   r.signguCode AS signguCode,
                   r.signguName AS signguName,
                   r.sidoName AS sidoName,
                   r.visitorCount AS visitorCount
            FROM RegionalVisitorStat r
            WHERE r.signguCode = :signguCode
              AND r.baseYmd BETWEEN :from AND :to
            ORDER BY r.baseYmd
            """)
    List<DailyVisitorProjection> findDailyBySignguCodeAndBaseYmdBetween(
            @Param("signguCode") String signguCode,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    interface RegionalYearProjection {
        String getSignguCode();
        String getSignguName();
        String getSidoName();
        Integer getStatYear();
        Long getVisitorCount();
    }

    interface DailyVisitorProjection {
        LocalDate getBaseYmd();
        String getSignguCode();
        String getSignguName();
        String getSidoName();
        Long getVisitorCount();
    }
}
