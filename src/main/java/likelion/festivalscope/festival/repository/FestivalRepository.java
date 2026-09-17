package likelion.festivalscope.festival.repository;

import likelion.festivalscope.festival.entity.Festival;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, Long> {
    Optional<Festival> findByFestivalCode(String festivalCode);
    List<Festival> findAllBySidoAndSigungu(String sido, String sigungu);
    List<Festival> findAllBySigungu(String sigungu);
}
