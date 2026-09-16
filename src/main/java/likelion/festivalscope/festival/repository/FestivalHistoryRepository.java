package likelion.festivalscope.festival.repository;

import likelion.festivalscope.festival.entity.FestivalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FestivalHistoryRepository extends JpaRepository<FestivalHistory, Long> {
    List<FestivalHistory> findAllByFestival_FestivalIdOrderByYearDesc(Long festivalId);
    Optional<FestivalHistory> findTopByFestival_FestivalIdAndVisitorCountIsNotNullOrderByYearDesc(Long festivalId);
    Optional<FestivalHistory> findByFestival_FestivalIdAndYear(Long festivalId, Integer year);
    List<FestivalHistory> findAllByVisitorCountIsNotNullOrderByYearDesc();
}
