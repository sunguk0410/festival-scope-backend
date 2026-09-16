package likelion.festivalscope.station.service;

import jakarta.transaction.Transactional;
import likelion.festivalscope.station.entity.Station;
import likelion.festivalscope.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class StationCsvDataLoader implements ApplicationRunner {
    private final StationRepository stationRepository;

    @Value("classpath:data/station.csv")
    private Resource stationCsv;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("Station CSV import started: resource={}, exists={}", stationCsv.getDescription(), stationCsv.exists());
        long existingCount = stationRepository.count();
        if (existingCount > 0) {
            log.info("Station CSV import skipped: station table already contains data, count={}", existingCount);
            return;
        }
        List<Station> stations = new ArrayList<>();
        int row = 1;
        for (Map<String, String> data : rows()) {
            row++;
            String stationId = required(data, "station_id", row, true);
            stations.add(Station.builder()
                    .stationId(Long.valueOf(stationId))
                    .stationName(required(data, "station_name", row, false))
                    .latitude(new BigDecimal(required(data, "latitude", row, false)))
                    .longitude(new BigDecimal(required(data, "longitude", row, false)))
                    .lineName(nullable(data.get("line_name")))
                    .build());
        }
        stationRepository.saveAllAndFlush(stations);
        log.info("Station CSV import completed: imported={}, databaseCount={}", stations.size(), stationRepository.count());
    }

    private List<Map<String, String>> rows() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stationCsv.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalStateException("station.csv: CSV header is missing");
            List<String> headers = parse(headerLine);
            headers.set(0, headers.get(0).replace("\uFEFF", ""));
            for (String header : List.of("station_id", "station_name", "latitude", "longitude", "line_name"))
                if (!headers.contains(header)) throw new IllegalStateException("station.csv: required header is missing: " + header);
            List<Map<String, String>> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                StringBuilder csvRecord = new StringBuilder(line);
                while (quoteCount(csvRecord) % 2 != 0) {
                    String continuation = reader.readLine();
                    if (continuation == null) throw new IllegalArgumentException("station.csv: unclosed quoted field");
                    csvRecord.append('\n').append(continuation);
                }
                List<String> values = parse(csvRecord.toString());
                Map<String, String> data = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) data.put(headers.get(i), i < values.size() ? values.get(i) : null);
                result.add(data);
            }
            return result;
        }
    }

    private List<String> parse(String line) {
        List<String> result = new ArrayList<>(); StringBuilder field = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; } else quoted = !quoted; }
            else if (c == ',' && !quoted) { result.add(field.toString()); field.setLength(0); } else field.append(c);
        }
        if (quoted) throw new IllegalArgumentException("station.csv: unclosed quoted field");
        result.add(field.toString()); return result;
    }

    private int quoteCount(CharSequence value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == '"') count++;
        return count;
    }

    private String required(Map<String, String> data, String column, int row, boolean numeric) {
        String value = nullable(data.get(column));
        if (value == null) throw new IllegalStateException("station.csv row " + row + ": required column is empty: " + column);
        if (numeric) try { Long.parseLong(value); } catch (NumberFormatException e) { throw new IllegalArgumentException("station.csv row " + row + ": invalid station_id: " + value, e); }
        return value;
    }

    private String nullable(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
