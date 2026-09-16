package likelion.festivalscope.station.service;

import likelion.festivalscope.analysis.dto.response.DemandFitResponse;
import likelion.festivalscope.common.util.GeoDistance;
import likelion.festivalscope.station.entity.Station;
import likelion.festivalscope.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StationAccessibilityService {
    private static final int WITHIN_ONE_KM = 1_000;
    private final StationRepository stationRepository;

    public DemandFitResponse.Rail analyze(BigDecimal latitude, BigDecimal longitude) {
        List<Nearby> all = stationRepository.findAll().stream()
                .map(station -> new Nearby(station, GeoDistance.meters(latitude, longitude, station.getLatitude(), station.getLongitude())))
                .sorted(Comparator.comparingInt(Nearby::distance).thenComparing(n -> n.station().getStationId()))
                .toList();
        List<DemandFitResponse.NearbyStation> nearby = all.stream()
                .filter(station -> station.distance() <= WITHIN_ONE_KM)
                .map(station -> new DemandFitResponse.NearbyStation(station.station().getStationName(), station.distance(), station.station().getLineName()))
                .toList();
        Nearby nearest = all.stream().findFirst().orElse(null);
        return new DemandFitResponse.Rail(nearest != null,
                nearest == null ? null : nearest.station().getStationName(),
                nearest == null ? null : nearest.distance(),
                nearest == null ? null : nearest.station().getLineName(),
                nearby.size(), nearby);
    }

    private record Nearby(Station station, int distance) {}
}
