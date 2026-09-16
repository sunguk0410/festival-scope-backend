package likelion.festivalscope.weather.repository;

import likelion.festivalscope.weather.entity.WeatherStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherStationRepository extends JpaRepository<WeatherStation, String> {
}
