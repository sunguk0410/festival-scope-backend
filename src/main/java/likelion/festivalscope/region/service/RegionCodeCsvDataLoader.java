package likelion.festivalscope.region.service;

import jakarta.transaction.Transactional;
import likelion.festivalscope.region.entity.RegionCode;
import likelion.festivalscope.region.repository.RegionCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
public class RegionCodeCsvDataLoader implements ApplicationRunner {
    private final RegionCodeRepository regionCodeRepository;

    @Value("classpath:data/region_code.csv")
    private Resource regionCodeCsv;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (regionCodeRepository.count() > 0) {
            log.info("Region code CSV import skipped: region_code table already contains data");
            return;
        }
        if (!regionCodeCsv.exists()) {
            log.warn("Region code CSV import skipped: resource is missing: {}", regionCodeCsv.getDescription());
            return;
        }
        List<RegionCode> codes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                regionCodeCsv.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) throw new IllegalStateException("region_code.csv header is missing");
            List<String> headers = parse(header);
            if (!headers.isEmpty()) headers.set(0, headers.get(0).replace("\uFEFF", ""));
            for (String required : List.of("sido_name", "sigungu_name", "area_cd", "sigungu_cd")) {
                if (!headers.contains(required)) throw new IllegalStateException("region_code.csv required header is missing: " + required);
            }
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                List<String> values = parse(line);
                String sido = value(headers, values, "sido_name", row);
                String sigungu = value(headers, values, "sigungu_name", row);
                String areaCd = value(headers, values, "area_cd", row);
                String sigunguCd = value(headers, values, "sigungu_cd", row);
                codes.add(RegionCode.builder().sidoName(sido).sigunguName(sigungu)
                        .areaCd(areaCd).sigunguCd(sigunguCd).build());
            }
        }
        regionCodeRepository.saveAllAndFlush(codes);
        log.info("Region code CSV import completed: imported={}", codes.size());
    }

    private String value(List<String> headers, List<String> values, String column, int row) {
        int index = headers.indexOf(column);
        if (index < 0 || index >= values.size() || values.get(index).trim().isEmpty()) {
            throw new IllegalArgumentException("region_code.csv row " + row + ": empty " + column);
        }
        return values.get(index).trim();
    }

    private List<String> parse(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                result.add(field.toString()); field.setLength(0);
            } else field.append(c);
        }
        result.add(field.toString());
        return result;
    }
}
