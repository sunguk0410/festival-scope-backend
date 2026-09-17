package likelion.festivalscope.analysis.service;

import jakarta.transaction.Transactional;
import likelion.festivalscope.analysis.entity.RegionalVisitorStat;
import likelion.festivalscope.analysis.repository.RegionalVisitorStatRepository;
import likelion.festivalscope.external.tourism.RegionalVisitorClient;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionalVisitorCollector {
    private final RegionalVisitorClient client;
    private final RegionalVisitorStatRepository repository;

    public void ensureRecentYearsCollected(int latestCompletedYear, int yearCount) {
        for (int year = latestCompletedYear; year > latestCompletedYear - yearCount; year--) {
            ensureYearCollected(year);
        }
    }

    @Transactional
    public void ensureYearCollected(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        long expectedDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate minDate = repository.findMinDate(from, to);
        LocalDate maxDate = repository.findMaxDate(from, to);
        long actualDays = repository.countDistinctDates(from, to);
        boolean completed = minDate != null
                && maxDate != null
                && minDate.equals(from)
                && maxDate.equals(to)
                && actualDays == expectedDays;
        if (completed) {
            log.info("Regional visitor collection skipped: year={}, minDate={}, maxDate={}, distinctDates={}",
                    year, minDate, maxDate, actualDays);
            return;
        }
        log.info("Regional visitor collection started: year={}, expectedDays={}, minDate={}, maxDate={}, distinctDates={}",
                year, expectedDays, minDate, maxDate, actualDays);
        List<RegionalVisitorClient.VisitorRecord> records = client.fetchAll(from, to);
        if (records.isEmpty()) {
            throw new AnalysisExecutionException("Regional visitor API returned no normalized records: year=" + year);
        }
        repository.deleteAllByBaseYmdBetween(from, to);
        List<RegionalVisitorStat> stats = records.stream().map(record -> RegionalVisitorStat.builder()
                .baseYmd(record.date())
                .signguCode(record.regionCode())
                .signguName(record.regionName())
                .sidoName(record.sidoName())
                .visitorCount(record.visitorCount())
                .build()).toList();
        repository.saveAll(stats);
        log.info("Regional visitor collection completed: year={}, recordCount={}", year, stats.size());
    }
}
