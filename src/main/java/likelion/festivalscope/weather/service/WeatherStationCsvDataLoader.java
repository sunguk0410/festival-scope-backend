package likelion.festivalscope.weather.service;

import jakarta.transaction.Transactional;
import likelion.festivalscope.weather.entity.WeatherStation;
import likelion.festivalscope.weather.repository.WeatherStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class WeatherStationCsvDataLoader implements ApplicationRunner {
    private final WeatherStationRepository repository;
    @Value("classpath:data/asos_stations.csv") private Resource resource;

    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header != null) header = header.replace("\uFEFF", "").trim();
            if (!"station_id,station_name,latitude,longitude".equals(header)) throw new IllegalStateException("asos-stations.csv header is invalid");
            reader.lines().filter(line -> !line.isBlank()).map(line -> line.split(",", -1)).forEach(v -> repository.save(WeatherStation.builder()
                    .stationId(v[0].trim()).stationName(v[1].trim()).latitude(new BigDecimal(v[2].trim())).longitude(new BigDecimal(v[3].trim())).build()));
        }
    }
}
