package likelion.festivalscope.analysis.service;

import likelion.festivalscope.external.tourism.RegionalVisitorClient;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.analysis.repository.RegionalVisitorStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class RegionalVisitorCollector {
    private static final long COMPLETED_YEAR_MIN_COUNT = 90_000L;

    private final RegionalVisitorClient client;
    private final RegionalVisitorStatRepository repository;
    private final RegionalVisitorBatchWriter batchWriter;
    private final int batchSize;

    public RegionalVisitorCollector(RegionalVisitorClient client,
                                    RegionalVisitorStatRepository repository,
                                    RegionalVisitorBatchWriter batchWriter,
                                    @org.springframework.beans.factory.annotation.Value("${tourism.visitor.batch-size:500}") int batchSize) {
        this.client = client;
        this.repository = repository;
        this.batchWriter = batchWriter;
        this.batchSize = batchSize;
    }

    public void ensureRecentYearsCollected(int latestCompletedYear, int yearCount) {
        for (int year = latestCompletedYear; year > latestCompletedYear - yearCount; year--) {
            ensureYearCollected(year);
        }
    }

    public void ensureYearCollected(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        long existingCount = repository.countByBaseYmdBetween(from, to);
        if (existingCount >= COMPLETED_YEAR_MIN_COUNT) {
            log.info("Regional visitor collection skipped: year={}, existingCount={}", year, existingCount);
            return;
        }

        long expectedDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate minDate = repository.findMinDate(from, to);
        LocalDate maxDate = repository.findMaxDate(from, to);
        long actualDays = repository.countDistinctDates(from, to);
        long rowsWithoutSido = repository.countWithoutSido(from, to);
        boolean completed = minDate != null
                && maxDate != null
                && minDate.equals(from)
                && maxDate.equals(to)
                && actualDays == expectedDays
                && rowsWithoutSido == 0;
        if (completed) {
            log.info("Regional visitor collection skipped: year={}, minDate={}, maxDate={}, distinctDates={}, rowsWithoutSido={}",
                    year, minDate, maxDate, actualDays, rowsWithoutSido);
            return;
        }
        log.info("Regional visitor collection started: year={}, expectedDays={}, minDate={}, maxDate={}, distinctDates={}, rowsWithoutSido={}",
                year, expectedDays, minDate, maxDate, actualDays, rowsWithoutSido);
        batchWriter.deleteYear(from, to);
        int[] savedCount = {0};
        client.fetchPagesWithMetadata(from, to, page -> {
            int saved = batchWriter.saveBatch(page.records(), batchSize);
            savedCount[0] += saved;
            log.info("Regional visitor page processed: year={}, page={}/{}, fetched={}, filtered={}, saved={}",
                    year, page.page(), page.totalPages(), page.fetched(), page.filtered(), saved);
        });
        if (savedCount[0] == 0) {
            throw new AnalysisExecutionException("Regional visitor API returned no normalized records: year=" + year);
        }
        log.info("Regional visitor collection completed: year={}, savedCount={}", year, savedCount[0]);
    }
}
