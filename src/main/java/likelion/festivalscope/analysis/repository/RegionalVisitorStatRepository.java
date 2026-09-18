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

    List<RegionalVisitorStat> findAllByBaseYmdBetween(LocalDate from, LocalDate to);
    List<RegionalVisitorStat> findAllBySignguNameAndBaseYmdBetween(String signguName, LocalDate from, LocalDate to);
    List<RegionalVisitorStat> findBySignguCodeAndBaseYmdBetween(String signguCode, LocalDate from, LocalDate to);
    List<RegionalVisitorStat> findAllBySignguCodeAndBaseYmdBetween(String signguCode, LocalDate from, LocalDate to);
    List<RegionalVisitorStat> findAllBySidoNameAndSignguNameAndBaseYmdBetween(String sidoName, String signguName,
                                                                                LocalDate from, LocalDate to);
}
