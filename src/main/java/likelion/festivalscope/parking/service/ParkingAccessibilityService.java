package likelion.festivalscope.parking.service;

import likelion.festivalscope.common.util.GeoDistance;
import likelion.festivalscope.parking.entity.Parking;
import likelion.festivalscope.parking.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service @RequiredArgsConstructor
public class ParkingAccessibilityService {
    private static final int WITHIN_ONE_KM = 1_000;
    private final ParkingRepository parkingRepository;

    public Result analyze(BigDecimal latitude, BigDecimal longitude) {
        int count = 0;
        int capacity = 0;
        for (Parking parking : parkingRepository.findAll()) {
            if (GeoDistance.meters(latitude, longitude, parking.getLatitude(), parking.getLongitude()) <= WITHIN_ONE_KM) {
                count++;
                capacity += parking.getCapacity();
            }
        }
        return new Result(count, capacity);
    }

    public record Result(int parkingCount, int parkingCapacity) {}
}
