package likelion.festivalscope.analysis.repository;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTargetVisitor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FestivalAnalysisTargetVisitorRepository extends JpaRepository<FestivalAnalysisTargetVisitor, Long> { Optional<FestivalAnalysisTargetVisitor> findByFestivalAnalysisItem_FestivalAnalysisItemId(Long itemId); }
