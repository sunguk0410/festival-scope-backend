package likelion.festivalscope.weather.service;

import likelion.festivalscope.common.util.GeoDistance;
import likelion.festivalscope.weather.entity.WeatherStation;
import likelion.festivalscope.weather.repository.WeatherStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class WeatherStationService {
    private final WeatherStationRepository repository;

    public Selection findNearest(BigDecimal latitude, BigDecimal longitude) {
        WeatherStation station = repository.findAll().stream()
                .min(Comparator.comparingInt(s -> GeoDistance.meters(latitude, longitude, s.getLatitude(), s.getLongitude())))
                .orElseThrow(() -> new IllegalStateException("ASOS station data is empty"));
        return new Selection(station, GeoDistance.meters(latitude, longitude, station.getLatitude(), station.getLongitude()) / 1000.0);
    }

    public record Selection(WeatherStation station, double distanceKm) {}
}
