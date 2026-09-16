package likelion.festivalscope.festival.service;

import jakarta.transaction.Transactional;
import likelion.festivalscope.festival.entity.Festival;
import likelion.festivalscope.festival.entity.FestivalHistory;
import likelion.festivalscope.festival.entity.FestivalTheme;
import likelion.festivalscope.festival.repository.FestivalHistoryRepository;
import likelion.festivalscope.festival.repository.FestivalRepository;
import likelion.festivalscope.festival.repository.FestivalThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalCsvDataLoader implements ApplicationRunner {
    private final FestivalRepository festivalRepository;
    private final FestivalHistoryRepository historyRepository;
    private final FestivalThemeRepository themeRepository;

    @Value("classpath:data/festivals.csv") private Resource festivalsCsv;
    @Value("classpath:data/festival_history.csv") private Resource historyCsv;
    @Value("classpath:data/festival_theme.csv") private Resource themeCsv;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Map<String, Festival> festivals = loadFestivals();
        int histories = loadHistories(festivals);
        int themes = loadThemes(festivals);
        log.info("Festival CSV import completed: festivals={}, histories={}, themes={}", festivals.size(), histories, themes);
    }

    private Map<String, Festival> loadFestivals() throws Exception {
        Map<String, Festival> result = new HashMap<>();
        int row = 1;
        for (Map<String, String> data : rows(festivalsCsv, "festival_code", "festival_name", "sido", "sigungu", "festival_type", "first_year", "venue")) {
            row++;
            String code = required(data, "festival_code", "festivals.csv", row);
            Festival festival = festivalRepository.findByFestivalCode(code).orElse(null);
            if (festival == null) {
                festival = festivalRepository.save(Festival.builder()
                        .festivalCode(code)
                        .festivalName(required(data, "festival_name", "festivals.csv", row))
                        .sido(required(data, "sido", "festivals.csv", row))
                        .sigungu(sigungu(data.get("sigungu")))
                        .festivalType(nullable(data.get("festival_type")))
                        .firstYear(integer(data.get("first_year"), "first_year", "festivals.csv", row))
                        .venue(nullable(data.get("venue")))
                        .build());
            }
            result.put(code, festival);
        }
        return result;
    }

    private int loadHistories(Map<String, Festival> festivals) throws Exception {
        int imported = 0, row = 1;
        for (Map<String, String> data : rows(historyCsv, "festival_code", "year", "festival_name_raw", "start_date", "end_date", "budget", "visitor_count", "venue_raw")) {
            row++;
            Festival festival = festival(festivals, required(data, "festival_code", "festival_history.csv", row), "festival_history.csv", row);
            Integer year = integer(required(data, "year", "festival_history.csv", row), "year", "festival_history.csv", row);
            if (historyRepository.findByFestival_FestivalIdAndYear(festival.getFestivalId(), year).isPresent()) continue;
            historyRepository.save(FestivalHistory.builder()
                    .festival(festival).year(year)
                    .festivalNameRaw(required(data, "festival_name_raw", "festival_history.csv", row))
                    .startDate(date(data.get("start_date"), "start_date", "festival_history.csv", row))
                    .endDate(date(data.get("end_date"), "end_date", "festival_history.csv", row))
                    .budget(decimal(data.get("budget"), "budget", "festival_history.csv", row))
                    .visitorCount(longValue(data.get("visitor_count"), "visitor_count", "festival_history.csv", row))
                    .venueRaw(nullable(data.get("venue_raw"))).build());
            imported++;
        }
        return imported;
    }

    private int loadThemes(Map<String, Festival> festivals) throws Exception {
        int imported = 0, row = 1;
        for (Map<String, String> data : rows(themeCsv, "festival_code", "theme_code", "theme_tag")) {
            row++;
            Festival festival = festival(festivals, required(data, "festival_code", "festival_theme.csv", row), "festival_theme.csv", row);
            String code = required(data, "theme_code", "festival_theme.csv", row);
            if (themeRepository.findByFestival_FestivalIdAndThemeCode(festival.getFestivalId(), code).isPresent()) continue;
            themeRepository.save(FestivalTheme.builder().festival(festival).themeCode(code)
                    .themeTag(required(data, "theme_tag", "festival_theme.csv", row)).build());
            imported++;
        }
        return imported;
    }

    private Festival festival(Map<String, Festival> festivals, String code, String file, int row) {
        Festival festival = festivals.get(code);
        if (festival == null) throw new IllegalStateException(file + " row " + row + ": festival_code='" + code + "' does not exist in festivals.csv");
        return festival;
    }

    private List<Map<String, String>> rows(Resource resource, String... requiredHeaders) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalStateException(resource.getFilename() + ": CSV header is missing");
            List<String> headers = parse(headerLine);
            headers.set(0, headers.get(0).replace("\uFEFF", ""));
            for (String header : requiredHeaders) if (!headers.contains(header)) throw new IllegalStateException(resource.getFilename() + ": required header is missing: " + header);
            List<Map<String, String>> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> values = parse(line);
                Map<String, String> data = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) data.put(headers.get(i), i < values.size() ? values.get(i) : null);
                result.add(data);
            }
            return result;
        }
    }

    private List<String> parse(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { result.add(field.toString()); field.setLength(0); }
            else field.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted CSV field: " + line);
        result.add(field.toString());
        return result;
    }

    private String required(Map<String, String> data, String column, String file, int row) {
        String value = nullable(data.get(column));
        if (value == null) throw new IllegalStateException(file + " row " + row + ": required column is empty: " + column);
        return value;
    }

    private String nullable(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null")) return null;
        return value.trim();
    }

    private String sigungu(String value) {
        String normalized = nullable(value);
        if (normalized == null || normalized.matches(".*(,|/|;|\\||·|&|\\s+및\\s+|\\s+외\\s+).*")) return null;
        return normalized;
    }

    private Integer integer(String value, String column, String file, int row) {
        String normalized = nullable(value); if (normalized == null) return null;
        try { return Integer.valueOf(normalized.replace(",", "")); } catch (NumberFormatException e) { throw invalid(column, normalized, file, row, e); }
    }

    private Long longValue(String value, String column, String file, int row) {
        String normalized = nullable(value); if (normalized == null) return null;
        try { return Long.valueOf(normalized.replace(",", "")); } catch (NumberFormatException e) { throw invalid(column, normalized, file, row, e); }
    }

    private BigDecimal decimal(String value, String column, String file, int row) {
        String normalized = nullable(value); if (normalized == null) return null;
        try { return new BigDecimal(normalized.replace(",", "")); } catch (NumberFormatException e) { throw invalid(column, normalized, file, row, e); }
    }

    private LocalDate date(String value, String column, String file, int row) {
        String normalized = nullable(value); if (normalized == null) return null;
        try { return LocalDate.parse(normalized); } catch (Exception e) { throw new IllegalArgumentException(file + " row " + row + ": invalid date in " + column + ": " + normalized, e); }
    }

    private IllegalArgumentException invalid(String column, String value, String file, int row, Exception cause) {
        return new IllegalArgumentException(file + " row " + row + ": invalid number in " + column + ": " + value, cause);
    }
}