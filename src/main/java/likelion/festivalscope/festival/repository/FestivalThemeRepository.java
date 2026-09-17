package likelion.festivalscope.festival.repository;

import likelion.festivalscope.festival.entity.FestivalTheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FestivalThemeRepository extends JpaRepository<FestivalTheme, Long> {
    List<FestivalTheme> findAllByFestival_FestivalId(Long festivalId);
    List<FestivalTheme> findAllByFestival_FestivalIdIn(Collection<Long> festivalIds);
    Optional<FestivalTheme> findByFestival_FestivalIdAndThemeCode(Long festivalId, String themeCode);
    Optional<FestivalTheme> findFirstByThemeCode(String themeCode);
}
