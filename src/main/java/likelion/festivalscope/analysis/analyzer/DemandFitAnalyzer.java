package likelion.festivalscope.analysis.analyzer;

import likelion.festivalscope.analysis.dto.response.DemandFitResponse;
import likelion.festivalscope.analysis.entity.FestivalAnalysisAccessibility;
import likelion.festivalscope.analysis.entity.FestivalAnalysisDemand;
import likelion.festivalscope.common.util.GeoDistance;
import likelion.festivalscope.external.bus.BusRouteClient;
import likelion.festivalscope.external.bus.BusStopClient;
import likelion.festivalscope.external.tourism.RegionalVisitorClient;
import likelion.festivalscope.global.exception.AnalysisExecutionException;
import likelion.festivalscope.global.exception.BusinessException;
import likelion.festivalscope.global.exception.ErrorCode;
import likelion.festivalscope.plan.entity.FestivalPlan;
import likelion.festivalscope.station.service.StationAccessibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DemandFitAnalyzer {
    private static final Map<String, String> AREA_CODES = Map.ofEntries(
            Map.entry("서울특별시", "1"), Map.entry("인천광역시", "2"), Map.entry("대전광역시", "3"),
            Map.entry("대구광역시", "4"), Map.entry("광주광역시", "5"), Map.entry("부산광역시", "6"),
            Map.entry("울산광역시", "7"), Map.entry("세종특별자치시", "8"), Map.entry("경기도", "31"),
            Map.entry("강원특별자치도", "32"), Map.entry("충청북도", "33"), Map.entry("충청남도", "34"),
            Map.entry("경상북도", "35"), Map.entry("경상남도", "36"), Map.entry("전북특별자치도", "37"),
            Map.entry("전라남도", "38"), Map.entry("제주특별자치도", "39"));
    private static final Map<String, String> ADMIN_CODE_PREFIXES = Map.ofEntries(
            Map.entry("1", "11"), Map.entry("2", "28"), Map.entry("3", "30"),
            Map.entry("4", "27"), Map.entry("5", "29"), Map.entry("6", "26"),
            Map.entry("7", "31"), Map.entry("8", "36"), Map.entry("31", "41"),
            Map.entry("32", "42"), Map.entry("33", "43"), Map.entry("34", "44"),
            Map.entry("35", "47"), Map.entry("36", "48"), Map.entry("37", "45"),
            Map.entry("38", "46"), Map.entry("39", "50"));

    private final RegionalVisitorClient visitorClient;
    private final BusStopClient busStopClient;
    private final BusRouteClient busRouteClient;
    private final StationAccessibilityService stationAccessibilityService;

    public Result analyze(FestivalPlan plan) {
        if (plan.getLatitude() == null || plan.getLongitude() == null) {
            throw new BusinessException(ErrorCode.VENUE_COORDINATE_REQUIRED);
        }
        if (plan.getSigungu() == null || plan.getSigungu().isBlank()) {
            throw new AnalysisExecutionException("시군구가 없어 지역 관광수요를 분석할 수 없습니다.");
        }
        int endYear = LocalDate.now().getYear() - 1;
        LocalDate from = LocalDate.of(endYear, 1, 1);
        LocalDate to = LocalDate.of(endYear, 12, 31);
        String area = AREA_CODES.get(plan.getSido());
        if (area == null) {
            throw new AnalysisExecutionException("시도 관광수요 지역 코드를 찾을 수 없습니다: " + plan.getSido());
        }
        List<RegionalVisitorClient.VisitorRecord> records = visitorClient.fetch(
                area, ADMIN_CODE_PREFIXES.get(area), from, to);
        // API가 0건을 반환한 경우에도 분석 자체는 성공시키되, 수요 지표는 null로 반환한다.
        AccessibilityData accessibility = accessibility(plan);
        return build(plan, records, accessibility, endYear);
    }

    public Result fromSnapshots(FestivalPlan plan, List<FestivalAnalysisDemand> rows,
                                FestivalAnalysisAccessibility saved) {
        List<RegionalYear> regionYears = rows.stream()
                .filter(r -> r.getDemandType().name().equals("REGIONAL") && r.getVisitorCount() != null)
                .map(r -> new RegionalYear(r.getSigungu(), r.getRegionCode(), r.getStatYear(), r.getVisitorCount()))
                .toList();
        List<RegionalVisitorClient.VisitorRecord> daily = rows.stream()
                .filter(r -> r.getDemandType().name().equals("SEASONAL")
                        && r.getStatMonth() != null && r.getStatDay() != null && r.getVisitorCount() != null)
                .map(r -> new RegionalVisitorClient.VisitorRecord(
                        LocalDate.of(r.getStatYear(), r.getStatMonth(), r.getStatDay()),
                        r.getRegionCode(), r.getSigungu(), r.getSido(), r.getVisitorCount()))
                .toList();
        AccessibilityData accessibility = new AccessibilityData(
                new DemandFitResponse.Bus(saved == null ? null : saved.getNearestBusStopName(),
                        saved == null ? null : saved.getNearestBusStopDistanceM(),
                        saved == null ? null : saved.getBusStopCount500m(),
                        saved == null ? null : saved.getBusStopCount1km(),
                        saved == null ? null : saved.getBusRouteCount()),
                stationAccessibilityService.analyze(plan.getLatitude(), plan.getLongitude()));
        return buildFromAggregates(plan, regionYears, daily, accessibility, LocalDate.now().getYear() - 1);
    }

    private Result build(FestivalPlan plan, List<RegionalVisitorClient.VisitorRecord> records,
                         AccessibilityData accessibility, int endYear) {
        Map<String, List<RegionalVisitorClient.VisitorRecord>> byRegion = records.stream()
                .collect(Collectors.groupingBy(RegionalVisitorClient.VisitorRecord::regionCode));
        List<RegionalYear> years = byRegion.values().stream()
                .flatMap(list -> list.stream()
                        .collect(Collectors.groupingBy(r -> r.regionCode() + "|" + r.date().getYear()))
                        .entrySet().stream()
                        .map(entry -> {
                            RegionalVisitorClient.VisitorRecord first = entry.getValue().get(0);
                            long total = entry.getValue().stream()
                                    .mapToLong(RegionalVisitorClient.VisitorRecord::visitorCount).sum();
                            return new RegionalYear(first.regionName(), first.regionCode(), first.date().getYear(), total);
                        }))
                .toList();
        List<RegionalVisitorClient.VisitorRecord> daily = records.stream()
                .filter(r -> r.regionName().equals(plan.getSigungu()))
                .toList();
        return buildFromAggregates(plan, years, daily, accessibility, endYear);
    }

    private Result buildFromAggregates(FestivalPlan plan, List<RegionalYear> years,
                                       List<RegionalVisitorClient.VisitorRecord> daily,
                                       AccessibilityData accessibility, int endYear) {
        String target = plan.getSigungu();
        String type = adminType(target);
        Map<String, Long> regionValues = years.stream()
                .filter(y -> adminType(y.name()).equals(type))
                .collect(Collectors.groupingBy(y -> y.code() + "|" + y.name(),
                        Collectors.summingLong(RegionalYear::value)));

        List<Map.Entry<String, Long>> ranked = regionValues.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).toList();
        String targetKey = findKey(regionValues, target);
        Long targetValue = targetKey == null ? null : regionValues.get(targetKey);
        List<Map.Entry<String, Long>> comparisonEntries = ranked.stream()
                .filter(entry -> !entry.getKey().equals(targetKey)).toList();
        List<DemandFitResponse.ComparisonRegion> comparisonRegions = comparisonEntries.stream()
                .map(entry -> new DemandFitResponse.ComparisonRegion(nameOf(entry.getKey()), entry.getValue(),
                        indexOf(ranked, entry.getKey()) + 1)).toList();

        // 비교지역이 없을 때 자기 자신을 비교군으로 만들거나 순위를 임의 생성하지 않는다.
        Integer rank = targetValue != null && !comparisonRegions.isEmpty() ? indexOf(ranked, targetKey) + 1 : null;
        Integer totalRegions = comparisonRegions.size();
        BigDecimal comparisonAverage = comparisonRegions.isEmpty() ? null
                : averageLong(comparisonEntries.stream().map(Map.Entry::getValue).toList());
        BigDecimal comparisonMedian = comparisonRegions.isEmpty() ? null
                : median(comparisonEntries.stream().map(Map.Entry::getValue).toList());
        BigDecimal percentile = rank == null ? null : percentile(rank, ranked.size());
        DemandFitResponse.RegionalDemand regional = new DemandFitResponse.RegionalDemand(
                target, targetValue, comparisonAverage, comparisonMedian, rank, totalRegions, percentile, comparisonRegions);

        int month = plan.getStartDate() == null ? 1 : plan.getStartDate().getMonthValue();
        Map<Integer, Long> monthly = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) monthly.put(m, monthlyAverage(daily, m, endYear));
        List<DemandFitResponse.MonthlyDemand> months = monthly.entrySet().stream()
                .map(entry -> new DemandFitResponse.MonthlyDemand(entry.getKey(), entry.getValue())).toList();
        List<Map.Entry<Integer, Long>> availableMonths = monthly.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed()).toList();
        Long eventMonthVisitorCount = monthly.get(month);
        Integer eventMonthRank = eventMonthVisitorCount == null ? null : indexOfMonth(availableMonths, month) + 1;
        BigDecimal eventMonthPercentile = eventMonthRank == null ? null
                : percentile(eventMonthRank, availableMonths.size());

        List<DemandFitResponse.WeeklyDemand> weeks = weekly(daily, month, endYear);
        Integer recommendedWeek = weeks.stream()
                .filter(w -> !w.referenceOnly() && w.rank() != null)
                .min(Comparator.comparing(DemandFitResponse.WeeklyDemand::rank))
                .map(DemandFitResponse.WeeklyDemand::week).orElse(null);
        DemandFitResponse.SeasonalDemand seasonal = new DemandFitResponse.SeasonalDemand(
                month, months, eventMonthVisitorCount, eventMonthRank, eventMonthPercentile, weeks, recommendedWeek);
        return new Result(regional, seasonal,
                new DemandFitResponse.Accessibility(accessibility.bus(), accessibility.rail()), years, daily);
    }

    private Long monthlyAverage(List<RegionalVisitorClient.VisitorRecord> daily, int month, int endYear) {
        boolean exists = daily.stream().anyMatch(r -> r.date().getMonthValue() == month
                && r.date().getYear() == endYear);
        if (!exists) return null;
        long sum = daily.stream().filter(r -> r.date().getMonthValue() == month
                        && r.date().getYear() == endYear)
                .mapToLong(RegionalVisitorClient.VisitorRecord::visitorCount).sum();
        return sum;
    }

    private List<DemandFitResponse.WeeklyDemand> weekly(List<RegionalVisitorClient.VisitorRecord> daily,
                                                         int month, int endYear) {
        List<DemandFitResponse.WeeklyDemand> raw = new ArrayList<>();
        Map<Integer, BigDecimal> values = new HashMap<>();
        for (int week = 1; week <= 5; week++) {
            int start = (week - 1) * 7 + 1;
            int end = Math.min(week * 7, YearMonth.of(endYear, month).lengthOfMonth());
            int days = Math.max(0, end - start + 1);
            boolean referenceOnly = days < 3;
            boolean exists = daily.stream().anyMatch(r -> r.date().getYear() == endYear
                    && r.date().getMonthValue() == month
                    && r.date().getDayOfMonth() >= start && r.date().getDayOfMonth() <= end);
            BigDecimal average = null;
            if (exists) {
                BigDecimal sum = BigDecimal.ZERO;
                long yearly = daily.stream().filter(r -> r.date().getYear() == endYear
                                    && r.date().getMonthValue() == month
                                    && r.date().getDayOfMonth() >= start && r.date().getDayOfMonth() <= end)
                            .mapToLong(RegionalVisitorClient.VisitorRecord::visitorCount).sum();
                average = BigDecimal.valueOf(yearly)
                        .divide(BigDecimal.valueOf(Math.max(days, 1)), 2, RoundingMode.HALF_UP);
                values.put(week, average);
            }
            raw.add(new DemandFitResponse.WeeklyDemand(week, start, end, days, average, null, referenceOnly));
        }
        List<Integer> order = values.entrySet().stream()
                .filter(entry -> !raw.get(entry.getKey() - 1).referenceOnly())
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .map(Map.Entry::getKey).toList();
        return raw.stream().map(w -> new DemandFitResponse.WeeklyDemand(w.week(), w.startDay(), w.endDay(), w.days(),
                w.averageDailyVisitors(), w.averageDailyVisitors() == null || w.referenceOnly()
                        ? null : order.indexOf(w.week()) + 1, w.referenceOnly())).toList();
    }

    private AccessibilityData accessibility(FestivalPlan plan) {
        List<BusStopClient.BusStop> stops = busStopClient.findNearby(plan.getLatitude(), plan.getLongitude(), 1000);
        BusStopClient.BusStop nearest = stops.stream()
                .min(Comparator.comparingInt(s -> GeoDistance.meters(plan.getLatitude(), plan.getLongitude(), s.latitude(), s.longitude())))
                .orElse(null);
        int routeCount = 0;
        for (BusStopClient.BusStop stop : stops) routeCount += busRouteClient.findRouteCount(stop.id(), stop.ctpvCode(), stop.sggCode());
        int near500 = (int) stops.stream().filter(s -> GeoDistance.meters(plan.getLatitude(), plan.getLongitude(), s.latitude(), s.longitude()) <= 500).count();
        DemandFitResponse.Rail rail = stationAccessibilityService.analyze(plan.getLatitude(), plan.getLongitude());
        return new AccessibilityData(
                new DemandFitResponse.Bus(nearest == null ? null : nearest.name(),
                        nearest == null ? null : GeoDistance.meters(plan.getLatitude(), plan.getLongitude(), nearest.latitude(), nearest.longitude()),
                        near500, stops.size(), routeCount),
                rail);
    }

    private String findKey(Map<String, Long> values, String name) {
        return values.keySet().stream().filter(key -> nameOf(key).equals(name)).findFirst().orElse(null);
    }

    private String nameOf(String key) { int index = key.indexOf('|'); return index < 0 ? key : key.substring(index + 1); }

    private int indexOf(List<Map.Entry<String, Long>> values, String key) {
        for (int i = 0; i < values.size(); i++) if (values.get(i).getKey().equals(key)) return i;
        return -1;
    }

    private int indexOfMonth(List<Map.Entry<Integer, Long>> values, int month) {
        for (int i = 0; i < values.size(); i++) if (values.get(i).getKey() == month) return i;
        return -1;
    }

    private String adminType(String name) {
        if (name == null) return "";
        if (name.endsWith("군")) return "군";
        if (name.endsWith("시")) return "시";
        if (name.endsWith("구")) return "구";
        return name;
    }

    private BigDecimal averageLong(List<Long> values) {
        return BigDecimal.valueOf(values.stream().mapToLong(Long::longValue).average().orElse(0)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? BigDecimal.valueOf(sorted.get(middle))
                : BigDecimal.valueOf(sorted.get(middle - 1)).add(BigDecimal.valueOf(sorted.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentile(int rank, int total) {
        return BigDecimal.valueOf((total - rank + 1) * 100D / total).setScale(2, RoundingMode.HALF_UP);
    }

    public record Result(DemandFitResponse.RegionalDemand regionalDemand,
                         DemandFitResponse.SeasonalDemand seasonalDemand,
                         DemandFitResponse.Accessibility accessibility,
                         List<RegionalYear> regionalYears,
                         List<RegionalVisitorClient.VisitorRecord> dailyRecords) {}
    public record RegionalYear(String name, String code, int year, long value) {}
    private record AccessibilityData(DemandFitResponse.Bus bus, DemandFitResponse.Rail rail) {}
}
