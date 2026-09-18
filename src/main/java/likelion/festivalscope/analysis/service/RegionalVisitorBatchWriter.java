package likelion.festivalscope.analysis.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import likelion.festivalscope.analysis.entity.RegionalVisitorStat;
import likelion.festivalscope.analysis.repository.RegionalVisitorStatRepository;
import likelion.festivalscope.external.tourism.RegionalVisitorClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionalVisitorBatchWriter {
    private final RegionalVisitorStatRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteYear(LocalDate from, LocalDate to) {
        repository.deleteAllByBaseYmdBetween(from, to);
        repository.flush();
        entityManager.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<RegionalVisitorClient.VisitorRecord> records, int batchSize) {
        if (records.isEmpty()) return 0;

        Set<String> keys = records.stream()
                .map(this::key)
                .collect(Collectors.toSet());
        Set<String> existingKeys = new HashSet<>(repository.findKeysByBaseYmdBetween(
                        records.stream().map(RegionalVisitorClient.VisitorRecord::date).min(LocalDate::compareTo).orElseThrow(),
                        records.stream().map(RegionalVisitorClient.VisitorRecord::date).max(LocalDate::compareTo).orElseThrow()));
        existingKeys.retainAll(keys);

        int saved = 0;
        for (int start = 0; start < records.size(); start += batchSize) {
            int end = Math.min(start + batchSize, records.size());
            List<RegionalVisitorStat> batch = records.subList(start, end).stream()
                    .filter(record -> !existingKeys.contains(key(record)))
                    .map(this::toEntity)
                    .toList();
            if (batch.isEmpty()) continue;
            repository.saveAll(batch);
            repository.flush();
            entityManager.clear();
            saved += batch.size();
        }
        return saved;
    }

    private RegionalVisitorStat toEntity(RegionalVisitorClient.VisitorRecord record) {
        return RegionalVisitorStat.builder()
                .baseYmd(record.date())
                .signguCode(record.regionCode())
                .signguName(record.regionName())
                .sidoName(record.sidoName())
                .visitorCount(record.visitorCount())
                .build();
    }

    private String key(RegionalVisitorClient.VisitorRecord record) {
        return record.date() + "|" + record.regionCode();
    }
}
