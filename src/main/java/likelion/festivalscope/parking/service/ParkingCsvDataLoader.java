package likelion.festivalscope.parking.service;

import jakarta.transaction.Transactional;
import likelion.festivalscope.parking.entity.Parking;
import likelion.festivalscope.parking.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j @Component @RequiredArgsConstructor @Order(2)
public class ParkingCsvDataLoader implements ApplicationRunner {
    private final ParkingRepository parkingRepository;
    @Value("classpath:data/parking.csv") private Resource parkingCsv;

    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        long existingCount = parkingRepository.count();
        if (existingCount > 0) {
            log.info("Parking CSV import skipped: parking table already contains data, count={}", existingCount);
            return;
        }
        List<Parking> parkings = new ArrayList<>();
        int row = 1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(parkingCsv.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.replace("\uFEFF", "").trim().equals("위도,경도,주차구획수")) {
                throw new IllegalStateException("parking.csv header is invalid");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] values = line.split(",", -1);
                if (values.length < 3) throw new IllegalArgumentException("parking.csv row " + row + ": columns are missing");
                parkings.add(Parking.builder()
                        .latitude(new BigDecimal(required(values[0], row, "위도")))
                        .longitude(new BigDecimal(required(values[1], row, "경도")))
                        .capacity(Integer.valueOf(required(values[2], row, "주차구획수")))
                        .build());
            }
        }
        parkingRepository.saveAllAndFlush(parkings);
        log.info("Parking CSV import completed: imported={}, databaseCount={}", parkings.size(), parkingRepository.count());
    }

    private String required(String value, int row, String column) {
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("parking.csv row " + row + ": empty " + column);
        return value.trim();
    }
}
