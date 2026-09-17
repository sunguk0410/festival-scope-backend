package likelion.festivalscope.region.repository;

import likelion.festivalscope.region.entity.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionCodeRepository extends JpaRepository<RegionCode, Long> {
    List<RegionCode> findAllBySidoNameAndSigunguName(String sidoName, String sigunguName);
}
