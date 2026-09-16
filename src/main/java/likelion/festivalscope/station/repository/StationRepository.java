package likelion.festivalscope.station.repository;

import likelion.festivalscope.station.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
}
