package likelion.festivalscope.parking.repository;

import likelion.festivalscope.parking.entity.Parking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingRepository extends JpaRepository<Parking, Long> {
}
