package likelion.festivalscope.analysis.service;

import likelion.festivalscope.analysis.analyzer.DemandFitAnalyzer;
import likelion.festivalscope.analysis.entity.*;
import likelion.festivalscope.analysis.dto.response.*;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.plan.entity.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResultInterpretationService {
    public InterpretationDecision interpretTourismLinkage(FestivalAnalysisTourismLinkage snapshot) {
        Integer tourismCount = snapshot.getTourismCultureWithin5kmCount();
        Integer foodShoppingCount = snapshot.getFoodShoppingWithin5kmCount();
        Integer accommodationCount = snapshot.getAccommodationWithin5kmCount();
        Integer within3kmCount = snapshot.getTourismCultureWithin3kmCount() == null
                || snapshot.getFoodShoppingWithin3kmCount() == null
                || snapshot.getAccommodationWithin3kmCount() == null ? null
                : snapshot.getTourismCultureWithin3kmCount()
                + snapshot.getFoodShoppingWithin3kmCount()
                + snapshot.getAccommodationWithin3kmCount();
        Integer within5kmCount = snapshot.getTotalCandidatePoiCount();

        String tourismSentence = tourismSentence(tourismCount);
        String consumptionSentence = consumptionSentence(foodShoppingCount);
        String staySentence = staySentence(accommodationCount);
        DistanceInterpretation distance = distanceInterpretation(within3kmCount, within5kmCount);
        int knownCategories = 0;
        int highPotentialCount = 0;
        int lowPotentialCount = 0;
        if (tourismCount != null) {
            knownCategories++;
            if (tourismCount >= 10) highPotentialCount++;
            if (tourismCount < 3) lowPotentialCount++;
        }
        if (foodShoppingCount != null) {
            knownCategories++;
            if (foodShoppingCount >= 10) highPotentialCount++;
            if (foodShoppingCount < 3) lowPotentialCount++;
        }
        if (accommodationCount != null) {
            knownCategories++;
            if (accommodationCount >= 5) highPotentialCount++;
            if (accommodationCount == 0) lowPotentialCount++;
        }
        if (knownCategories < 3) {
            return decision("데이터 부족", new ResultInterpretation(
                    "주변 관광 연계 잠재력을 판단하기 위한 POI 집계가 일부 부족합니다.",
                    joinTourismSentences(tourismSentence, consumptionSentence, staySentence, distance.sentence())),
                    List.of(metric("totalPoiWithin5km", within5kmCount), metric("tourismCultureWithin5km", tourismCount),
                            metric("foodShoppingWithin5km", foodShoppingCount), metric("accommodationWithin5km", accommodationCount)));
        }

        String summary;
        String conclusion;
        if (highPotentialCount >= 2) {
            summary = "행사장 주변에 다양한 관광자원이 분포해 축제 방문을 지역 관광으로 확장할 잠재력이 높은 편입니다.";
            conclusion = "주변 관광·문화자원과 지역 상권을 함께 활용할 수 있는 기반이 형성되어 있어, 축제 방문 자체에 그치지 않고 관광·소비·체류 활동으로 연결할 수 있는 가능성이 높습니다.";
        } else if (highPotentialCount == 1 && lowPotentialCount == 0) {
            summary = "주변 관광자원을 활용한 연계 가능성이 있으며 일부 영역에서 강점이 확인됩니다.";
            conclusion = "모든 영역에서 연계 기반이 강한 것은 아니지만, 특정 관광자원을 중심으로 축제 방문을 주변 관광 활동으로 확장할 수 있는 여건이 형성되어 있습니다.";
        } else if (highPotentialCount == 0 && lowPotentialCount == 0) {
            summary = "관광·소비·체류 연계를 위한 기본적인 주변 자원은 확보되어 있습니다.";
            conclusion = "뚜렷하게 강한 연계 자원이 집중된 구조는 아니지만, 축제와 주변 관광자원을 연결할 수 있는 기본적인 여건은 마련되어 있습니다.";
        } else if (lowPotentialCount == 1) {
            summary = "주변 관광자원과의 연계 가능성은 있으나 일부 영역의 기반이 부족합니다.";
            conclusion = "관광 연계가 가능한 자원은 존재하지만 특정 영역의 기반이 상대적으로 부족해, 축제 방문을 관광·소비·체류 전반으로 확장하는 데에는 일부 제약이 있습니다.";
        } else {
            summary = "행사장 주변의 관광 연계 자원이 제한적이어서 지역 관광으로의 확장 여건이 약한 편입니다.";
            conclusion = "행사장 주변에서 활용할 수 있는 관광·소비·체류 자원이 전반적으로 부족해, 현재 입지에서는 축제 방문을 주변 관광 활동으로 자연스럽게 확장하기 어려운 구조입니다.";
        }
        String detail = joinTourismSentences(tourismSentence, consumptionSentence, staySentence, distance.sentence());
        return decision(tourismStatus(highPotentialCount, lowPotentialCount),
                new ResultInterpretation(summary, (detail + " " + conclusion).trim()),
                List.of(metric("totalPoiWithin5km", within5kmCount), metric("tourismCultureWithin5km", tourismCount),
                        metric("foodShoppingWithin5km", foodShoppingCount), metric("accommodationWithin5km", accommodationCount),
                        metric("highPotentialCount", highPotentialCount), metric("lowPotentialCount", lowPotentialCount)));
    }

    private String tourismStatus(int high, int low) {
        if (high >= 2) return "연계 잠재력 높음";
        if (high == 1 && low == 0) return "보통";
        if (high == 0 && low == 0) return "보통";
        return "취약";
    }

    private InterpretationMetric metric(String key, Object value) {
        return new InterpretationMetric(key, value);
    }

    private InterpretationDecision decision(String status, ResultInterpretation interpretation,
                                            List<InterpretationMetric> metrics) {
        ResultInterpretation formatted = new ResultInterpretation(
                interpretation.summary(),
                interpretation.detail());
        return new InterpretationDecision(status, formatted, metrics);
    }

    private String tourismSentence(Integer count) {
        if (count == null) return "";
        if (count >= 10) return String.format("행사장 5km 내 관광·문화자원이 %d개 확인되어 축제와 주변 관광지를 연계할 수 있는 자원 기반이 충분한 편입니다. 축제 방문을 행사장 내부에 한정하지 않고 주변 관광 활동으로 확장할 수 있는 여건이 형성되어 있습니다.", count);
        if (count >= 3) return String.format("행사장 5km 내 관광·문화자원이 %d개 확인됩니다. 주변 관광지를 축제 전후 일정과 연계할 수 있는 기본적인 자원은 확보되어 있으나, 선택 가능한 관광 동선은 다소 제한적일 수 있습니다.", count);
        return String.format("행사장 5km 내 확인되는 관광·문화자원은 %d개로 제한적입니다. 축제와 가까운 거리에서 관광 코스를 구성할 수 있는 자원이 많지 않아, 주변 관광지와의 직접적인 연계 여건은 상대적으로 약한 편입니다.", count);
    }

    private String consumptionSentence(Integer count) {
        if (count == null) return "";
        if (count >= 10) return String.format("행사장 5km 내 음식·쇼핑 자원이 %d개 확인되어 축제 방문객의 소비를 주변 상권으로 연결할 수 있는 기반이 충분한 편입니다. 축제 방문이 식음·쇼핑 등 지역 소비 활동으로 이어질 가능성이 높습니다.", count);
        if (count >= 3) return String.format("행사장 5km 내 음식·쇼핑 자원이 %d개 확인됩니다. 축제 방문객의 소비를 주변 상권으로 연결할 수 있는 기본적인 환경은 형성되어 있으나, 연계 가능한 상권의 범위는 다소 제한적일 수 있습니다.", count);
        return String.format("행사장 5km 내 확인되는 음식·쇼핑 자원은 %d개로 제한적입니다. 행사장 인근에서 방문객 소비를 지역 상권으로 자연스럽게 연결할 수 있는 여건은 상대적으로 약한 편입니다.", count);
    }

    private String staySentence(Integer count) {
        if (count == null) return "";
        if (count >= 5) return String.format("행사장 5km 내 숙박시설이 %d개 확인되어 축제 방문을 숙박과 연계할 수 있는 기반이 충분한 편입니다. 당일 방문뿐 아니라 체류형 관광으로 확장할 수 있는 여건이 형성되어 있습니다.", count);
        if (count >= 1) return String.format("행사장 5km 내 숙박시설이 %d개 확인되어 기본적인 숙박 연계는 가능한 것으로 보입니다. 다만 이용 가능한 숙박 자원이 많지는 않아 대규모 방문객의 체류 수요를 수용하는 데에는 한계가 있을 수 있습니다.", count);
        return "행사장 5km 내 확인되는 숙박시설이 없어 축제 방문을 숙박과 직접 연계할 수 있는 여건은 제한적입니다. 현재 주변 자원만을 기준으로는 당일 방문을 체류형 관광으로 확장하기 어려운 구조입니다.";
    }

    private DistanceInterpretation distanceInterpretation(Integer within3kmCount, Integer within5kmCount) {
        if (within3kmCount == null || within5kmCount == null || within5kmCount <= 0) return new DistanceInterpretation("");
        BigDecimal percentage = BigDecimal.valueOf(within3kmCount)
                .divide(BigDecimal.valueOf(within5kmCount), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        String value = formatInterpretation(percentage);
        if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) return new DistanceInterpretation(String.format("행사장 5km 내 전체 연계 자원 중 약 %s%%가 3km 이내에 위치해 있어, 방문객이 축제와 주변 관광·소비 활동을 하나의 동선으로 연결하기에 유리한 편입니다.", value));
        if (percentage.compareTo(BigDecimal.valueOf(40)) >= 0) return new DistanceInterpretation(String.format("행사장 5km 내 연계 자원 중 약 %s%%가 3km 이내에 위치하고 있습니다. 가까운 거리에도 활용 가능한 자원이 존재하지만 일부 자원은 추가 이동이 필요한 구조입니다.", value));
        return new DistanceInterpretation(String.format("행사장 5km 내 연계 자원 중 3km 이내에 위치한 비율은 약 %s%%로 낮은 편입니다. 주변 자원 자체는 존재하더라도 실제 축제 방문 동선과 연결하려면 추가적인 이동이 필요할 수 있습니다.", value));
    }

    private String joinTourismSentences(String... sentences) {
        return Arrays.stream(sentences).filter(sentence -> sentence != null && !sentence.isBlank())
                .collect(Collectors.joining("\n")).trim();
    }

    private record DistanceInterpretation(String sentence) {}

    public InterpretationDecision interpretTargetVisitor(
            FestivalPlan plan, Long targetVisitorCount, List<FestivalAnalysisSimilar> rows) {
        if (targetVisitorCount == null || targetVisitorCount <= 0) {
            return decision("데이터 부족", fallbackInterpretation("목표 방문객 수와 비교 기준을 확인할 수 없어 결과를 해석하기 어렵습니다."), List.of());
        }

        BigDecimal target = BigDecimal.valueOf(targetVisitorCount);
        BigDecimal similarMedian = medianForInterpretation(rows, TargetVisitorComparisonType.SIMILAR_FESTIVAL);
        if (plan.getFestivalStatus() == FestivalStatus.NEW) {
            if (!validMedian(similarMedian)) {
                return decision("데이터 부족", fallbackInterpretation("유사 축제 방문객 비교 기준이 없어 목표 방문객 규모를 해석하기 어렵습니다."), List.of());
            }
            BigDecimal gap = interpretationGap(target, similarMedian);
            return decision(targetStatus(gap), new ResultInterpretation(
                    newSummaryForNew(gap),
                    newDetailForNew(target, similarMedian, gap)), targetMetrics(target, similarMedian, gap));
        }

        if (plan.getFestivalStatus() != FestivalStatus.EXISTING) {
            return decision("데이터 부족", fallbackInterpretation("축제 유형을 확인할 수 없어 목표 방문객 규모를 해석하기 어렵습니다."), List.of());
        }

        BigDecimal historyMedian = medianForInterpretation(rows, TargetVisitorComparisonType.SAME_FESTIVAL);
        if (!validMedian(historyMedian)) {
            return decision("데이터 부족", fallbackInterpretation("해당 축제의 과거 방문 이력이 없어 목표 방문객 규모를 해석하기 어렵습니다."), List.of());
        }

        BigDecimal historyGap = interpretationGap(target, historyMedian);
        BigDecimal similarGap = validMedian(similarMedian) ? interpretationGap(target, similarMedian) : null;
        String trendSentence = historyTrendSentence(rows);
        return decision(targetStatus(historyGap), new ResultInterpretation(
                summaryForExisting(historyGap),
                detailForExisting(target, historyMedian, historyGap, similarMedian, similarGap, trendSentence)),
                targetMetrics(target, historyMedian, historyGap));
    }

    private List<InterpretationMetric> targetMetrics(BigDecimal target, BigDecimal median, BigDecimal gap) {
        return List.of(metric("comparisonMedian", median), metric("gapRate", gap),
                metric("targetRatio", target.divide(median, 6, RoundingMode.HALF_UP)));
    }

    private String targetStatus(BigDecimal gap) {
        if (gap.compareTo(BigDecimal.valueOf(50)) >= 0) return "과다 설정";
        if (gap.compareTo(BigDecimal.valueOf(20)) >= 0) return "다소 높음";
        if (gap.compareTo(BigDecimal.valueOf(-20)) >= 0) return "적정";
        if (gap.compareTo(BigDecimal.valueOf(-50)) >= 0) return "다소 낮음";
        return "보수적 설정";
    }

    private ResultInterpretation fallbackInterpretation(String detail) {
        return new ResultInterpretation("목표 방문객 규모를 해석하기 위한 비교 기준이 부족합니다.", detail);
    }

    private BigDecimal medianForInterpretation(List<FestivalAnalysisSimilar> rows,
                                               TargetVisitorComparisonType comparisonType) {
        List<Long> values = rows.stream()
                .filter(row -> row.getComparisonType() == comparisonType)
                .map(FestivalAnalysisSimilar::getVisitorCount)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (values.isEmpty()) return null;
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) return BigDecimal.valueOf(values.get(middle));
        return BigDecimal.valueOf(values.get(middle - 1))
                .add(BigDecimal.valueOf(values.get(middle)))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private boolean validMedian(BigDecimal median) {
        return median != null && median.signum() > 0;
    }

    private BigDecimal interpretationGap(BigDecimal target, BigDecimal median) {
        return target.subtract(median)
                .divide(median, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String newSummaryForNew(BigDecimal gap) {
        if (gap.compareTo(BigDecimal.valueOf(50)) >= 0) return "목표 방문객 규모가 유사 축제 실적을 크게 상회합니다.";
        if (gap.compareTo(BigDecimal.valueOf(20)) >= 0) return "목표 방문객 규모가 유사 축제보다 다소 높게 설정되어 있습니다.";
        if (gap.compareTo(BigDecimal.valueOf(-20)) >= 0) return "목표 방문객 규모가 유사 축제의 일반적인 실적 수준과 유사합니다.";
        if (gap.compareTo(BigDecimal.valueOf(-50)) >= 0) return "목표 방문객 규모가 유사 축제보다 보수적으로 설정되어 있습니다.";
        return "목표 방문객 규모가 유사 축제 실적에 비해 매우 낮게 설정되어 있습니다.";
    }

    private String newDetailForNew(BigDecimal target, BigDecimal median, BigDecimal gap) {
        String targetText = formatInterpretation(target);
        String medianText = formatInterpretation(median);
        if (gap.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 약 %s%% 높게 설정되어 있습니다. 유사한 주제와 규모의 축제에서 일반적으로 관측된 방문객 수준을 크게 넘어서는 목표이므로, 현재 목표치를 전제로 한 수요 및 운영 계획은 근거가 충분하지 않을 수 있습니다.", targetText, medianText, formatInterpretation(gap));
        }
        if (gap.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 약 %s%% 높은 수준입니다. 유사 축제의 일반적인 방문 규모를 상회하고 있어 다소 도전적인 목표로 볼 수 있으며, 현재 목표치를 뒷받침할 추가적인 방문 유입 요인이 있는지 함께 확인할 필요가 있습니다.", targetText, medianText, formatInterpretation(gap));
        }
        if (gap.compareTo(BigDecimal.valueOf(-20)) >= 0) {
            return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명과 약 %s%% 차이를 보입니다. 유사한 주제와 규모의 축제에서 실제로 관측된 방문객 수준과 큰 차이가 없어, 현재 목표는 비교적 현실적인 범위로 볼 수 있습니다.", targetText, medianText, formatInterpretation(gap.abs()));
        }
        if (gap.compareTo(BigDecimal.valueOf(-50)) >= 0) {
            return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 약 %s%% 낮게 설정되어 있습니다. 유사 축제의 일반적인 방문 규모보다 낮은 수준으로, 현재 목표는 달성 가능성을 중시해 비교적 보수적으로 설정된 것으로 볼 수 있습니다.", targetText, medianText, formatInterpretation(gap.abs()));
        }
        return String.format("현재 목표 %s명은 유사 축제 방문객 중앙값 %s명보다 약 %s%% 낮은 수준입니다. 유사한 주제와 규모의 축제에서 관측된 방문객 수준과 차이가 크게 나타나므로, 현재 목표가 실제 행사 규모를 충분히 반영하고 있는지 확인할 필요가 있습니다.", targetText, medianText, formatInterpretation(gap.abs()));
    }

    private String summaryForExisting(BigDecimal gap) {
        if (gap.compareTo(BigDecimal.valueOf(50)) >= 0) return "목표 방문객 규모가 해당 축제의 기존 실적을 크게 상회합니다.";
        if (gap.compareTo(BigDecimal.valueOf(20)) >= 0) return "목표 방문객 규모가 해당 축제의 기존 실적보다 다소 높게 설정되어 있습니다.";
        if (gap.compareTo(BigDecimal.valueOf(-20)) >= 0) return "목표 방문객 규모가 해당 축제의 기존 실적과 유사한 수준입니다.";
        if (gap.compareTo(BigDecimal.valueOf(-50)) >= 0) return "목표 방문객 규모가 해당 축제의 기존 실적보다 보수적으로 설정되어 있습니다.";
        return "목표 방문객 규모가 해당 축제의 기존 실적에 비해 매우 낮게 설정되어 있습니다.";
    }

    private String detailForExisting(BigDecimal target, BigDecimal historyMedian, BigDecimal historyGap,
                                     BigDecimal similarMedian, BigDecimal similarGap, String trendSentence) {
        String base;
        String targetText = formatInterpretation(target);
        String historyText = formatInterpretation(historyMedian);
        if (historyGap.compareTo(BigDecimal.valueOf(50)) >= 0) {
            base = String.format("현재 목표 %s명은 해당 축제의 전체 방문 이력 중앙값 %s명보다 약 %s%% 높게 설정되어 있습니다. 기존 개최 실적과 비교해 큰 폭의 성장이 필요한 수준이며, ", targetText, historyText, formatInterpretation(historyGap));
        } else if (historyGap.compareTo(BigDecimal.valueOf(20)) >= 0) {
            base = String.format("현재 목표 %s명은 해당 축제의 전체 방문 이력 중앙값 %s명보다 약 %s%% 높은 수준입니다. 기존 실적보다 뚜렷한 성장이 필요한 목표이며, ", targetText, historyText, formatInterpretation(historyGap));
        } else if (historyGap.compareTo(BigDecimal.valueOf(-20)) >= 0) {
            base = String.format("현재 목표 %s명은 해당 축제의 전체 방문 이력 중앙값 %s명과 약 %s%% 차이를 보입니다. 기존 개최 실적과 큰 차이가 없어 현재 목표는 과거 성과를 기준으로 비교적 현실적인 범위에 있습니다. ", targetText, historyText, formatInterpretation(historyGap.abs()));
        } else if (historyGap.compareTo(BigDecimal.valueOf(-50)) >= 0) {
            base = String.format("현재 목표 %s명은 해당 축제의 전체 방문 이력 중앙값 %s명보다 약 %s%% 낮게 설정되어 있습니다. 기존 개최 실적보다 낮은 수준으로, 현재 목표는 달성 가능성을 중시해 비교적 보수적으로 설정된 것으로 볼 수 있습니다. ", targetText, historyText, formatInterpretation(historyGap.abs()));
        } else {
            base = String.format("현재 목표 %s명은 해당 축제의 전체 방문 이력 중앙값 %s명보다 약 %s%% 낮은 수준입니다. 기존 개최 실적과 차이가 크게 나타나는 만큼, 현재 목표가 실제 행사 규모를 충분히 반영하고 있는지 확인할 필요가 있습니다. ", targetText, historyText, formatInterpretation(historyGap.abs()));
        }
        if (similarMedian != null && similarGap != null) {
            if (historyGap.compareTo(BigDecimal.valueOf(50)) >= 0) {
                base += String.format("유사 축제 방문객 중앙값 %s명과 비교해도 약 %s%% 높은 수준입니다. ",
                        formatInterpretation(similarMedian), formatInterpretation(similarGap));
            } else {
                base += String.format("유사 축제 방문객 중앙값 %s명과 비교하면 약 %s%% 차이를 보입니다. ",
                        formatInterpretation(similarMedian), formatInterpretation(similarGap.abs()));
            }
        }
        return joinInterpretationLines(base, trendSentence);
    }

    private String historyTrendSentence(List<FestivalAnalysisSimilar> rows) {
        List<FestivalAnalysisSimilar> histories = rows.stream()
                .filter(row -> row.getComparisonType() == TargetVisitorComparisonType.SAME_FESTIVAL
                        && row.getVisitorCount() != null && row.getYear() != null)
                .sorted(Comparator.comparing(FestivalAnalysisSimilar::getYear))
                .toList();
        if (histories.size() < 2) return "";
        long first = histories.get(0).getVisitorCount();
        long last = histories.get(histories.size() - 1).getVisitorCount();
        if (last > first) return "최근 확보된 방문 이력에서는 방문객 수가 전반적으로 증가하는 흐름을 보여, 기존 실적보다 높은 목표를 일부 뒷받침할 수 있습니다.";
        if (last < first) return "최근 확보된 방문 이력에서는 방문객 수가 전반적으로 감소하는 흐름을 보여, 기존 실적보다 높은 목표를 설정할 경우 이를 뒷받침할 추가적인 유입 근거가 필요합니다.";
        return "최근 확보된 방문 이력에서는 방문객 규모가 큰 변화 없이 유지되고 있어, 기존 실적을 크게 상회하는 목표를 뒷받침할 뚜렷한 성장 흐름은 확인되지 않습니다.";
    }

    private String formatInterpretation(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    public InterpretationDecision interpretDemandFit(
            FestivalPlan plan, DemandFitAnalyzer.Result result) {
        DemandFitResponse.RegionalDemand regional = result.regionalDemand();
        DemandFitResponse.SeasonalDemand seasonal = result.seasonalDemand();
        if (regional == null || regional.percentile() == null
                || seasonal == null || seasonal.eventMonthPercentile() == null) {
            return decision("데이터 부족", new ResultInterpretation(
                    "지역 또는 개최 시기 관광수요를 해석하기 위한 데이터가 부족합니다.",
                    "지역 관광수요와 개최월 관광수요 백분위가 모두 확보되지 않아 현재 개최 조건을 종합적으로 해석하기 어렵습니다."), List.of());
        }

        BigDecimal regionPercentile = regional.percentile();
        BigDecimal monthPercentile = seasonal.eventMonthPercentile();
        String regionSentence = demandRegionSentence(regionPercentile);
        String monthSentence = demandMonthSentence(seasonal);
        String weekSentence = demandWeekSentence(plan, seasonal);
        String accessibilitySentence = demandAccessibilitySentence(result.accessibility());
        String summary = demandSummary(regionPercentile, monthPercentile);
        String detail = joinInterpretationLines(regionSentence, monthSentence, weekSentence, accessibilitySentence,
                demandConclusion(regionPercentile, monthPercentile));
        return decision(demandStatus(regionPercentile, monthPercentile), new ResultInterpretation(summary, detail),
                List.of(metric("regionPercentile", regionPercentile), metric("monthPercentile", monthPercentile),
                        metric("eventMonthRank", seasonal.eventMonthRank()), metric("currentWeekRank", currentWeekRank(plan, seasonal))));
    }

    private String demandStatus(BigDecimal region, BigDecimal month) {
        boolean highRegion = region.compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean mediumRegion = region.compareTo(BigDecimal.valueOf(40)) >= 0;
        boolean highMonth = month.compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean mediumMonth = month.compareTo(BigDecimal.valueOf(40)) >= 0;
        if (highRegion && highMonth) return "수요 우수";
        if (!highRegion && highMonth) return "시기 강점";
        if (highRegion && mediumMonth) return "지역 강점";
        if (mediumRegion && mediumMonth) return "보통";
        if (highRegion) return "지역 강점";
        return "수요 취약";
    }

    private Integer currentWeekRank(FestivalPlan plan, DemandFitResponse.SeasonalDemand seasonal) {
        if (plan.getStartDate() == null || seasonal.weeklyDemand() == null) return null;
        int week = (plan.getStartDate().getDayOfMonth() - 1) / 7 + 1;
        return seasonal.weeklyDemand().stream().filter(item -> item.week() == week)
                .map(DemandFitResponse.WeeklyDemand::rank).findFirst().orElse(null);
    }

    private String demandRegionSentence(BigDecimal percentile) {
        String value = formatInterpretation(percentile);
        if (percentile.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return String.format("개최 지역의 관광수요는 비교 권역 기준 %s백분위로 높은 수준입니다. 비교 지역 중 상위권에 해당해, 축제 개최 이전부터 기본적인 관광객 유입 기반이 충분히 형성되어 있는 지역으로 볼 수 있습니다.", value);
        }
        if (percentile.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return String.format("개최 지역의 관광수요는 비교 권역 기준 %s백분위로 중간 수준입니다. 주변 지역과 비교했을 때 기본적인 관광객 유입 기반은 확보되어 있으나, 뚜렷하게 높은 수준은 아닙니다.", value);
        }
        return String.format("개최 지역의 관광수요는 비교 권역 기준 %s백분위로 낮은 수준입니다. 주변 지역과 비교했을 때 기본적인 관광객 유입 기반이 상대적으로 약해, 축제 자체가 외부 방문 수요를 추가로 만들어낼 필요가 있습니다.", value);
    }

    private String demandMonthSentence(DemandFitResponse.SeasonalDemand seasonal) {
        String month = seasonal.eventMonth() == null ? null : String.valueOf(seasonal.eventMonth());
        String percentile = formatInterpretation(seasonal.eventMonthPercentile());
        if (month == null) return "개최 예정 월 정보가 없어 월별 관광수요를 해석하기 어렵습니다.";
        if (seasonal.eventMonthPercentile().compareTo(BigDecimal.valueOf(75)) >= 0) {
            return String.format("개최 예정 월인 %s월의 관광수요는 연중 %s백분위 수준이며, 월별 관광수요 순위는 %s위입니다. 해당 시기는 연중 관광객 유입이 상대적으로 활발한 시기로, 개최 시기 자체의 수요 기반은 양호합니다.", month, percentile, valueOrUnavailable(seasonal.eventMonthRank()));
        }
        if (seasonal.eventMonthPercentile().compareTo(BigDecimal.valueOf(40)) >= 0) {
            return String.format("개최 예정 월인 %s월의 관광수요는 연중 %s백분위 수준으로, 월별 관광수요는 평균적인 수준입니다. 개최 시기 자체가 뚜렷한 강점이나 약점으로 작용하는 시기는 아닌 것으로 볼 수 있습니다.", month, percentile);
        }
        return String.format("개최 예정 월인 %s월의 관광수요는 연중 %s백분위 수준으로 낮은 편입니다. 해당 시기는 다른 시기에 비해 기본적인 관광객 유입이 적어, 축제 자체의 방문 유인 효과가 더 중요하게 작용할 수 있습니다.", month, percentile);
    }

    private String demandWeekSentence(FestivalPlan plan, DemandFitResponse.SeasonalDemand seasonal) {
        if (seasonal.recommendedWeek() == null || plan.getStartDate() == null
                || seasonal.weeklyDemand() == null) return "";
        int currentWeek = (plan.getStartDate().getDayOfMonth() - 1) / 7 + 1;
        DemandFitResponse.WeeklyDemand current = seasonal.weeklyDemand().stream()
                .filter(week -> week.week() == currentWeek)
                .findFirst().orElse(null);
        DemandFitResponse.WeeklyDemand recommended = seasonal.weeklyDemand().stream()
                .filter(week -> week.week() == seasonal.recommendedWeek())
                .findFirst().orElse(null);
        if (current == null || recommended == null || current.averageDailyVisitors() == null
                || recommended.averageDailyVisitors() == null
                || recommended.averageDailyVisitors().signum() <= 0) return "";

        BigDecimal gap = current.averageDailyVisitors().subtract(recommended.averageDailyVisitors())
                .divide(recommended.averageDailyVisitors(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        String currentValue = formatInterpretation(current.averageDailyVisitors());
        String recommendedValue = formatInterpretation(recommended.averageDailyVisitors());
        if (gap.compareTo(BigDecimal.valueOf(-10)) >= 0) {
            return String.format("현재 개최 예정인 %d주차의 평균 일 방문객은 %s명으로, 해당 월에서 관광수요가 가장 높은 %d주차의 %s명과 큰 차이가 없습니다. 현재 일정도 월 내 관광수요가 높은 구간에 포함됩니다.", currentWeek, currentValue, seasonal.recommendedWeek(), recommendedValue);
        }
        if (gap.compareTo(BigDecimal.valueOf(-25)) >= 0) {
            return String.format("현재 개최 예정인 %d주차의 평균 일 방문객은 %s명으로, 관광수요가 가장 높은 %d주차의 %s명보다 약 %s%% 낮습니다. 개최월 전체의 수요와 별개로 같은 달 안에서는 현재 일정이 최적의 수요 구간보다 다소 낮은 수준입니다.", currentWeek, currentValue, seasonal.recommendedWeek(), recommendedValue, formatInterpretation(gap.abs()));
        }
        return String.format("현재 개최 예정인 %d주차의 평균 일 방문객은 %s명으로, 관광수요가 가장 높은 %d주차의 %s명보다 약 %s%% 낮습니다. 동일한 개최월 내에서도 주차별 관광수요 차이가 크게 나타나, 현재 일정은 상대적으로 수요가 낮은 구간에 해당합니다.", currentWeek, currentValue, seasonal.recommendedWeek(), recommendedValue, formatInterpretation(gap.abs()));
    }

    private String demandAccessibilitySentence(DemandFitResponse.Accessibility accessibility) {
        if (accessibility == null) return "";
        DemandFitResponse.Bus bus = accessibility.bus();
        DemandFitResponse.Rail rail = accessibility.rail();
        DemandFitResponse.Parking parking = accessibility.parking();
        int secured = 0;
        if (bus != null && ((bus.routeCount() != null && bus.routeCount() > 0)
                || (bus.stopCount1km() != null && bus.stopCount1km() > 0))) secured++;
        if (rail != null && Boolean.TRUE.equals(rail.available())
                && rail.nearestStationDistanceM() != null && rail.nearestStationDistanceM() <= 3000) secured++;
        if (parking != null && parking.parkingCount() != null && parking.parkingCount() > 0
                && parking.parkingCapacity() != null && parking.parkingCapacity() > 0) secured++;
        if (bus == null && rail == null && parking == null) return "행사장 접근성 데이터가 부족해 접근 조건을 해석하기 어렵습니다.";

        String busSentence = bus == null ? "" : String.format("반경 1km 내 버스 정류장 %s개와 %s개 노선이 확인되고", valueOrUnavailable(bus.stopCount1km()), valueOrUnavailable(bus.routeCount()));
        String railSentence = rail == null || rail.nearestStationDistanceM() == null ? "철도역 거리 정보는 확인되지 않으며" : String.format("최근접 철도역은 약 %skm 거리에 있으며", formatInterpretation(BigDecimal.valueOf(rail.nearestStationDistanceM()).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP)));
        String parkingSentence = parking == null ? "주차장 정보는 확인되지 않습니다." : String.format("인근에는 주차장 %s개, 총 %s면의 주차 공간이 확인됩니다.", valueOrUnavailable(parking.parkingCount()), valueOrUnavailable(parking.parkingCapacity()));
        if (secured == 3) return "행사장 접근 조건도 전반적으로 양호합니다. " + busSentence + " " + railSentence + " " + parkingSentence;
        if (secured == 2) return "행사장 접근수단은 일부 확보되어 있으나 교통수단별 편의성에는 차이가 있습니다. " + busSentence + " " + railSentence + " " + parkingSentence + " 특정 이동수단에 대한 의존도가 높을 수 있습니다.";
        return "행사장 접근성은 상대적으로 취약한 편입니다. " + busSentence + " " + railSentence + " " + parkingSentence + " 대중교통 접근이 제한될 경우 외부 방문객의 이동 부담이 발생할 수 있습니다.";
    }

    private String demandSummary(BigDecimal region, BigDecimal month) {
        boolean highRegion = region.compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean averageRegion = region.compareTo(BigDecimal.valueOf(40)) >= 0;
        boolean highMonth = month.compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean averageMonth = month.compareTo(BigDecimal.valueOf(40)) >= 0;
        if (highRegion && highMonth) return "개최 지역과 시기 모두 관광수요 기반이 양호합니다.";
        if (averageRegion && highMonth) return "개최 시기의 높은 관광수요가 지역의 평균적인 수요 기반을 보완하고 있습니다.";
        if (!averageRegion && highMonth) return "개최 시기의 관광수요는 높지만 지역 자체의 관광객 유입 기반은 약한 편입니다.";
        if (highRegion && averageMonth) return "지역의 관광객 유입 기반은 강하지만 개최 시기의 수요는 평균적인 수준입니다.";
        if (averageRegion && averageMonth) return "개최 지역과 시기의 관광수요는 모두 평균적인 수준입니다.";
        if (!averageRegion && averageMonth) return "개최 지역의 관광수요 기반은 약하지만 개최 시기는 평균적인 수준입니다.";
        if (highRegion) return "지역의 관광수요 기반은 강하지만 개최 시기의 관광수요는 낮은 편입니다.";
        if (averageRegion) return "지역의 기본 관광수요는 확보되어 있지만 개최 시기의 수요는 낮은 편입니다.";
        return "개최 지역과 시기 모두 기본 관광수요가 상대적으로 낮은 편입니다.";
    }

    private String demandConclusion(BigDecimal region, BigDecimal month) {
        boolean highRegion = region.compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean averageRegion = region.compareTo(BigDecimal.valueOf(40)) >= 0;
        boolean highMonth = month.compareTo(BigDecimal.valueOf(75)) >= 0;
        boolean averageMonth = month.compareTo(BigDecimal.valueOf(40)) >= 0;
        if (highRegion && highMonth) return "지역 자체의 관광객 유입 기반과 개최 시기의 수요가 모두 긍정적으로 확인되어 축제 개최를 뒷받침할 기본적인 관광수요 조건이 잘 형성되어 있습니다.";
        if (averageRegion && highMonth) return "지역 자체의 수요 강점은 크지 않지만, 개최 시기 선택은 관광수요 측면에서 긍정적으로 작용하고 있습니다.";
        if (!averageRegion && highMonth) return "지역 자체의 기본 관광수요는 상대적으로 약하지만, 수요가 높은 시기를 선택한 점은 이를 일부 보완할 수 있습니다.";
        if (highRegion && averageMonth) return "지역이 보유한 기본 관광객 유입 기반이 축제 수요를 뒷받침하는 주요 조건으로 작용할 수 있습니다.";
        if (averageRegion && averageMonth) return "지역과 개최 시기 모두 기본적인 관광객 유입 기반은 형성되어 있으나, 축제 수요를 크게 높일 만한 뚜렷한 관광수요 강점은 확인되지 않습니다.";
        if (!averageRegion && averageMonth) return "시기적으로 특별히 불리하지는 않지만 지역 자체의 관광객 유입 기반이 약해, 축제가 자체적으로 추가 방문 수요를 만들어내는 역할이 중요합니다.";
        if (highRegion) return "지역 자체에는 충분한 관광객 유입 기반이 있지만, 현재 개최 시기는 해당 지역의 관광수요 강점을 충분히 활용하기 어려운 시기에 해당합니다.";
        if (averageRegion) return "지역 자체의 기본적인 관광객 유입은 존재하지만, 현재 개최 시기는 다른 시기에 비해 기존 관광수요를 활용하기 불리한 조건입니다.";
        return "지역 자체의 관광객 유입 기반과 개최 시기의 기존 관광수요가 모두 약해, 현재 조건에서는 축제 자체가 새로운 방문 동기를 만들어내는 역할이 특히 중요합니다.";
    }

    public InterpretationDecision interpretWeatherRisk(WeatherRiskResponse response) {
        if (response.analysisPeriod() == null || response.rain() == null
                || response.temperature() == null || response.wind() == null) {
            return decision("데이터 부족", new ResultInterpretation("과거 동일 시기 기상 이력을 해석하기 위한 데이터가 부족합니다.",
                    "강수, 온도 또는 강풍의 과거 동일 시기 관측값이 충분하지 않아 기상 리스크를 종합적으로 해석하기 어렵습니다."), List.of());
        }

        int actualYears = response.analysisPeriod().actualYears();
        String rainSentence = weatherRainSentence(response.rain(), actualYears);
        TemperatureRisk temperatureRisk = temperatureRisk(response.temperature(), actualYears);
        String temperatureSentence = weatherTemperatureSentence(response.temperature(), temperatureRisk, actualYears);
        String windSentence = weatherWindSentence(response.wind(), actualYears);
        String spaceSentence = weatherSpaceSentence(response.festivalCondition());
        int highRiskCount = actualYears <= 0 ? 0
                : highRiskCount(response.rain().occurrenceRate(), temperatureRisk.rate(), response.wind().strongWindOccurrenceRate());
        int moderateRiskCount = actualYears <= 0 ? 0
                : moderateRiskCount(response.rain().occurrenceRate(), temperatureRisk.rate(), response.wind().strongWindOccurrenceRate());
        VenueType spaceType = response.festivalCondition() == null ? null : response.festivalCondition().spaceType();

        if (spaceType == null) {
            return decision("기상 영향 제한", new ResultInterpretation("과거 기상 이력은 확인되지만 행사 공간 유형이 없어 영향을 종합하기 어렵습니다.",
                    joinWeatherSentences(rainSentence, temperatureSentence, windSentence)),
                    weatherMetrics(response, temperatureRisk, highRiskCount, moderateRiskCount, spaceType));
        }
        String summary = weatherSummary(highRiskCount, moderateRiskCount, spaceType);
        String conclusion = weatherConclusion(highRiskCount, spaceType);
        String detail = joinWeatherSentences(rainSentence, temperatureSentence, windSentence, spaceSentence, conclusion);
        return decision(weatherStatus(spaceType, highRiskCount, moderateRiskCount), new ResultInterpretation(summary, detail),
                weatherMetrics(response, temperatureRisk, highRiskCount, moderateRiskCount, spaceType));
    }

    private List<InterpretationMetric> weatherMetrics(WeatherRiskResponse response, TemperatureRisk temperatureRisk,
                                                      int highRiskCount, int moderateRiskCount, VenueType spaceType) {
        return List.of(metric("rainOccurrenceRate", response.rain().occurrenceRate()),
                metric("temperatureType", temperatureRisk.type()), metric("temperatureOccurrenceRate", temperatureRisk.rate()),
                metric("windOccurrenceRate", response.wind().strongWindOccurrenceRate()), metric("spaceType", spaceType),
                metric("highRiskCount", highRiskCount), metric("moderateRiskCount", moderateRiskCount));
    }

    private String weatherStatus(VenueType spaceType, int high, int moderate) {
        if (spaceType == VenueType.INDOOR) return "기상 영향 제한";
        if (spaceType == VenueType.OUTDOOR && high >= 2) return "기상 리스크 높음";
        if (high >= 1 || moderate >= 2) return "기상 주의";
        return "기상 리스크 낮음";
    }

    private String weatherRainSentence(WeatherRiskResponse.Rain rain, int actualYears) {
        if (rain.occurrenceRate() == null || actualYears <= 0) return "";
        String rate = formatInterpretation(rain.occurrenceRate());
        if (rain.occurrenceRate().compareTo(BigDecimal.valueOf(60)) >= 0) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 강수가 발생해, 강수 발생 비율은 %s%%로 높은 편입니다. 유효 관측일 기준 강수일 비율도 %s%%로 확인되어 해당 시기는 우천 영향을 반복적으로 받을 가능성이 있는 시기로 볼 수 있습니다.", actualYears, rain.occurrenceYears(), rate, valueOrUnavailable(rain.rainDayRate()));
        }
        if (rain.occurrenceRate().compareTo(BigDecimal.valueOf(30)) >= 0) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 강수가 발생해, 강수 발생 비율은 %s%%입니다. 매년 반복적으로 비가 발생하는 수준은 아니지만 행사 운영 시 우천 가능성을 고려할 필요가 있는 시기입니다.", actualYears, rain.occurrenceYears(), rate);
        }
        return String.format("최근 %d년 중 동일 시기에 강수가 발생한 연도는 %d개로, 강수 발생 비율은 %s%%입니다. 과거 데이터를 기준으로 강수가 반복적으로 발생하는 시기는 아닌 것으로 확인됩니다.", actualYears, rain.occurrenceYears(), rate);
    }

    private TemperatureRisk temperatureRisk(WeatherRiskResponse.Temperature temperature, int actualYears) {
        BigDecimal hot = temperature.hotOccurrenceRate();
        BigDecimal cold = temperature.coldOccurrenceRate();
        if (hot == null && cold == null) return new TemperatureRisk(null, TemperatureType.NEUTRAL);
        if (hot == null) return new TemperatureRisk(cold, TemperatureType.COLD);
        if (cold == null) return new TemperatureRisk(hot, TemperatureType.HOT);
        if (hot.compareTo(cold) > 0) return new TemperatureRisk(hot, TemperatureType.HOT);
        if (cold.compareTo(hot) > 0) return new TemperatureRisk(cold, TemperatureType.COLD);
        return new TemperatureRisk(hot, TemperatureType.NEUTRAL);
    }

    private String weatherTemperatureSentence(WeatherRiskResponse.Temperature temperature,
                                              TemperatureRisk risk, int actualYears) {
        if (risk.rate() == null || actualYears <= 0) return "";
        if (risk.rate().compareTo(BigDecimal.valueOf(30)) < 0) {
            return "최근 동일 시기에는 반복적으로 나타나는 뚜렷한 고온·저온 위험이 확인되지 않았습니다.";
        }
        String rate = formatInterpretation(risk.rate());
        boolean high = risk.rate().compareTo(BigDecimal.valueOf(60)) >= 0;
        if (risk.type() == TemperatureType.HOT) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 고온이 발생해, 고온 발생 비율은 %s%%%s. 동일 시기의 평균 최고기온도 %s℃로 나타나 장시간 야외 활동 시 더위가 주요 운영 변수로 작용할 가능성이 있습니다.", actualYears, temperature.hotOccurrenceYears(), rate, high ? "로 높은 편입니다" : "입니다", valueOrUnavailable(temperature.averageMaxTemperature()));
        }
        if (risk.type() == TemperatureType.COLD) {
            if (high) return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 저온이 발생해, 저온 발생 비율은 %s%%로 높은 편입니다. 동일 시기의 평균 최저기온은 %s℃로 나타나 야외 프로그램 운영 시 추위가 주요 운영 변수로 작용할 가능성이 있습니다.", actualYears, temperature.coldOccurrenceYears(), rate, valueOrUnavailable(temperature.averageMinTemperature()));
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 저온이 발생해, 저온 발생 비율은 %s%%입니다. 반복적인 저온 위험이 매우 높은 수준은 아니지만 야외 행사 운영 시 추위에 따른 체류 부담을 고려할 필요가 있습니다.", actualYears, temperature.coldOccurrenceYears(), rate);
        }
        return String.format("최근 %d년 동일 시기에는 고온과 저온 발생 비율이 각각 %s%%로 나타나 특정 온도 위험이 우세하지 않았습니다.", actualYears, rate);
    }

    private String weatherWindSentence(WeatherRiskResponse.Wind wind, int actualYears) {
        if (wind.strongWindOccurrenceRate() == null || actualYears <= 0) return "";
        String rate = formatInterpretation(wind.strongWindOccurrenceRate());
        if (wind.strongWindOccurrenceRate().compareTo(BigDecimal.valueOf(60)) >= 0) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 강풍이 발생해, 강풍 발생 비율은 %s%%로 높은 편입니다. 관측된 최대 풍속은 %sm/s로, 야외 시설물과 무대 운영에 영향을 줄 수 있는 기상 변수로 볼 수 있습니다.", actualYears, wind.strongWindOccurrenceYears(), rate, valueOrUnavailable(wind.maxWindSpeed()));
        }
        if (wind.strongWindOccurrenceRate().compareTo(BigDecimal.valueOf(30)) >= 0) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 강풍이 발생해, 강풍 발생 비율은 %s%%입니다. 반복적인 강풍 위험이 높은 수준은 아니지만 야외 시설물과 프로그램 운영 시 고려할 필요가 있습니다.", actualYears, wind.strongWindOccurrenceYears(), rate);
        }
        return String.format("최근 동일 시기에 강풍이 발생한 연도는 %d개로, 강풍 발생 비율은 %s%%입니다. 과거 데이터를 기준으로 강풍이 반복적으로 행사 운영에 영향을 줄 가능성은 낮은 편입니다.", wind.strongWindOccurrenceYears(), rate);
    }

    private String weatherSpaceSentence(WeatherRiskResponse.FestivalCondition condition) {
        if (condition == null || condition.spaceType() == null) return "";
        return switch (condition.spaceType()) {
            case OUTDOOR -> "현재 축제는 야외 행사로 계획되어 있어 강수, 기온, 강풍과 같은 기상 변수가 프로그램 운영과 방문객 체류에 직접적인 영향을 줄 수 있습니다.";
            case MIXED -> "현재 축제는 실내·야외 혼합형 행사로 계획되어 있어 일부 기상 영향을 분산할 수 있지만, 야외 프로그램은 강수와 기온, 강풍에 영향을 받을 수 있습니다.";
            case INDOOR -> "현재 축제는 실내 중심 행사로 계획되어 있어 외부 기상 변화가 전체 프로그램 운영에 미치는 직접적인 영향은 상대적으로 제한적입니다.";
        };
    }

    private int highRiskCount(BigDecimal... rates) {
        return (int) Arrays.stream(rates).filter(rate -> rate != null && rate.compareTo(BigDecimal.valueOf(60)) >= 0).count();
    }

    private int moderateRiskCount(BigDecimal... rates) {
        return (int) Arrays.stream(rates).filter(rate -> rate != null
                && rate.compareTo(BigDecimal.valueOf(30)) >= 0
                && rate.compareTo(BigDecimal.valueOf(60)) < 0).count();
    }

    private String weatherSummary(int high, int moderate, VenueType spaceType) {
        if (spaceType == VenueType.OUTDOOR) {
            if (high >= 2) return "과거 기상 이력과 행사 구조를 고려하면 기상 변수에 대한 취약성이 높은 편입니다.";
            if (high == 1) return "특정 기상 요인이 야외 행사 운영의 주요 위험 요소로 확인됩니다.";
            if (moderate >= 2) return "뚜렷한 고위험 기상 요소는 없지만 여러 기상 변수를 함께 고려할 필요가 있습니다.";
            return "과거 데이터를 기준으로 전반적인 기상 리스크는 낮은 편입니다.";
        }
        if (spaceType == VenueType.MIXED) {
            if (high >= 1) return "기상 위험 요소가 확인되지만 실내·야외 혼합 운영으로 일부 영향을 분산할 수 있습니다.";
            return "과거 기상 위험은 크지 않으며 혼합형 행사 구조로 기상 대응 여건도 비교적 양호합니다.";
        }
        return "기상 위험 요소는 존재할 수 있으나 행사 운영에 미치는 직접적인 영향은 제한적인 편입니다.";
    }

    private String weatherConclusion(int high, VenueType spaceType) {
        if (spaceType == VenueType.OUTDOOR) {
            if (high >= 2) return "동일 시기에 반복적으로 나타난 주요 기상 위험 요소가 여러 개 확인되고 행사가 야외 중심으로 계획되어 있어, 기상 변화가 실제 프로그램 운영과 방문객 체류에 영향을 줄 가능성이 높습니다.";
            if (high == 1) return "전체 기상 조건이 모두 불리한 것은 아니지만, 반복적으로 발생한 특정 기상 위험이 야외 프로그램 운영에 직접적인 영향을 줄 가능성이 있습니다.";
            if (high == 0) return "개별 기상 요인의 위험이 매우 높은 수준은 아니지만, 과거 기상 이력만을 기준으로 보면 행사 운영에 대한 전반적인 기상 부담은 상대적으로 낮습니다.";
        }
        if (spaceType == VenueType.MIXED) {
            if (high >= 1) return "과거 동일 시기에 주의가 필요한 기상 요소가 확인되지만, 실내 공간을 함께 활용하는 행사 구조로 인해 야외 단독 행사보다 기상 변화에 대한 취약성은 상대적으로 낮을 수 있습니다.";
            return "반복적으로 나타나는 고위험 기상 요소가 확인되지 않았고 실내 공간도 함께 활용할 수 있어, 기상 변화가 전체 행사 운영에 미치는 영향은 비교적 제한적일 것으로 볼 수 있습니다.";
        }
        return "동일 시기의 기상 특성은 방문객 이동 등에 영향을 줄 수 있지만, 실내 중심 행사 특성상 프로그램 자체가 기상 변화로 중단되거나 축소될 가능성은 상대적으로 낮습니다.";
    }

    private String joinWeatherSentences(String... sentences) {
        return Arrays.stream(sentences).filter(sentence -> sentence != null && !sentence.isBlank())
                .collect(Collectors.joining("\n")).trim();
    }

    private String joinInterpretationLines(String... sentences) {
        return Arrays.stream(sentences)
                .filter(sentence -> sentence != null && !sentence.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private enum TemperatureType { HOT, COLD, NEUTRAL }
    private record TemperatureRisk(BigDecimal rate, TemperatureType type) {}

    public InterpretationDecision interpretConflictRisk(
            FestivalAnalysisConflict snapshot, List<ConflictRiskResponse.Event> events) {
        Integer directCount = snapshot.getDirectOverlapCount();
        Integer nearbyCount = snapshot.getNearbyPeriodCount();
        if (directCount == null || nearbyCount == null) {
            return decision("데이터 부족", new ResultInterpretation(
                    "과거 행사 이력을 해석하기 위한 데이터가 부족합니다.",
                    "과거 동일 날짜 또는 인접 시기 행사 수가 확인되지 않아 일정 집중 패턴을 해석하기 어렵습니다."), List.of());
        }

        Integer historyYears = historyYears(snapshot.getHistoryStartYear(), snapshot.getHistoryEndYear());
        int historicalEventYears = historicalEventYears(events);
        BigDecimal historicalRate = historyYears == null || historyYears == 0 ? null
                : BigDecimal.valueOf(historicalEventYears * 100L)
                .divide(BigDecimal.valueOf(historyYears), 2, RoundingMode.HALF_UP);
        String directSentence = conflictDirectOverlapSentence(directCount);
        String nearbySentence = conflictNearbyPeriodSentence(nearbyCount);
        String historicalSentence = conflictHistoricalSentence(historyYears, historicalEventYears, historicalRate);
        String regionSentence = conflictRegionSentence(snapshot.getSameRegionCount());
        String themeSentence = conflictThemeSentence(events);
        String summary = conflictSummaryV2(directCount, nearbyCount, historicalEventYears, historicalRate);
        String detail = joinInterpretationLines(directSentence, nearbySentence, historicalSentence,
                regionSentence, themeSentence,
                conflictConclusionV2(directCount, nearbyCount, historicalEventYears, historicalRate));
        String status = conflictStatus(directCount, nearbyCount, historicalRate);
        return decision(status, new ResultInterpretation(summary, detail),
                List.of(metric("directOverlapCount", directCount), metric("nearbyPeriodCount", nearbyCount),
                        metric("possibleConflictCount", directCount + nearbyCount), metric("historicalEventYears", historicalEventYears),
                        metric("historyYears", historyYears)));
    }

    private String conflictStatus(int direct, int nearby, BigDecimal historicalRate) {
        if (direct >= 2) return "중복 리스크 높음";
        if (direct == 1 || nearby >= 3 || (historicalRate != null && historicalRate.compareTo(BigDecimal.valueOf(60)) >= 0)) {
            return "중복 주의";
        }
        return "중복 리스크 낮음";
    }

    private Integer historyYears(Integer startYear, Integer endYear) {
        if (startYear == null || endYear == null || endYear < startYear) return null;
        return endYear - startYear + 1;
    }

    private int historicalEventYears(List<ConflictRiskResponse.Event> events) {
        return (int) events.stream()
                .filter(event -> event.eventBasis() == EventBasis.HISTORICAL && event.eventYear() != null)
                .map(ConflictRiskResponse.Event::eventYear)
                .distinct()
                .count();
    }

    private String directOverlapSentence(int count) {
        if (count >= 2) {
            return String.format("과거 동일 날짜 구간에 주변 행사 %d건이 개최된 이력이 확인되었습니다. 같은 시기에 여러 행사가 함께 열렸던 사례가 있어, 향후에도 유사한 일정 집중이 발생할 가능성을 고려할 필요가 있습니다.", count);
        }
        if (count == 1) {
            return "과거 동일 날짜 구간에 주변 행사 1건이 개최된 이력이 확인되었습니다. 반복적인 일정 집중이라고 보기는 어렵지만, 향후 일정 수립 시 참고할 수 있는 과거 사례입니다.";
        }
        return "과거 동일 날짜 구간에 주변 행사가 개최된 이력은 확인되지 않았습니다.";
    }

    private String nearbyPeriodSentence(int count) {
        if (count >= 3) {
            return String.format("과거 개최 예정일 전후의 인접 시기에 주변 행사 %d건이 확인되었습니다. 직접적으로 같은 날짜는 아니더라도 비슷한 시기에 행사가 집중된 이력이 있어, 향후 일정 중복 가능성을 함께 고려할 필요가 있습니다.", count);
        }
        if (count >= 1) {
            return String.format("과거 개최 예정일 전후의 인접 시기에 주변 행사 %d건이 확인되었습니다. 행사 집중도가 높은 수준은 아니지만 향후 일정 수립 시 참고할 수 있는 이력이 존재합니다.", count);
        }
        return "과거 개최 예정일 전후의 인접 시기에도 주변 행사 이력이 확인되지 않아, 해당 시기에 행사가 집중되는 경향은 낮은 편입니다.";
    }

    private String historicalSentence(Integer historyYears, int eventYears, BigDecimal rate) {
        if (historyYears == null || rate == null) return "과거 동일 시기 반복 이력은 분석 기간을 확인할 수 없어 해석하지 않았습니다.";
        if (eventYears == 0) {
            return String.format("최근 %d년 동안 동일 시기에 개최된 주변 행사 이력이 확인되지 않아, 과거 반복 패턴에 따른 일정 중복 가능성은 낮은 편입니다.", historyYears);
        }
        if (rate.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 주변 행사가 확인되었습니다. 과거 같은 시기에 행사가 반복적으로 집중된 이력이 있어 향후에도 일정 중복 가능성을 고려할 필요가 있습니다.", historyYears, eventYears);
        }
        if (rate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return String.format("최근 %d년 중 %d개 연도에서 동일 시기에 주변 행사가 확인되었습니다. 반복적인 행사 집중이 뚜렷한 수준은 아니지만, 운영 및 홍보 계획 수립 시 참고할 만한 과거 이력이 존재합니다.", historyYears, eventYears);
        }
        return String.format("최근 %d년 동안 동일 시기의 주변 행사 이력은 제한적으로 확인되었습니다. 과거 기록만으로 해당 시기에 행사가 반복적으로 집중된다고 보기는 어렵습니다.", historyYears);
    }

    private String regionConflictSentence(Integer count) {
        if (count == null) return "동일 지역 행사 여부는 확인되지 않았습니다.";
        if (count >= 2) return String.format("확인된 과거 행사 중 %d건이 동일 지역에서 개최된 이력이 있어, 향후 유사한 시기에 행사가 다시 집중될 경우 방문객과 교통 수요가 직접적으로 겹칠 가능성이 있습니다.", count);
        if (count == 1) return "확인된 과거 행사 중 1건이 동일 지역에서 개최된 이력이 있습니다. 반복적인 지역 집중으로 보기는 어렵지만, 향후 일정이 확정될 경우 지역 내 방문 및 교통 수요 변화를 함께 확인할 필요가 있습니다.";
        return "확인된 과거 행사 중 동일 지역에서 개최된 사례는 없어, 지역 내 직접적인 수요 충돌 가능성은 낮은 편입니다.";
    }

    private String themeSentence(List<ConflictRiskResponse.Event> events) {
        long sameThemeCount = events.stream()
                .filter(event -> Boolean.TRUE.equals(event.sameTheme()))
                .count();
        if (sameThemeCount == 0) return "";
        return String.format("확인된 과거 행사 중 %d건은 현재 기획과 유사한 테마를 가지고 있어, 향후 일정이 겹칠 경우 유사한 방문객 수요가 분산될 가능성이 있습니다.", sameThemeCount);
    }

    private String conflictDirectOverlapSentence(int count) {
        if (count >= 2) {
            return String.format("기획한 개최 기간과 동일한 날짜 구간에 과거 주변 행사 %d건이 함께 개최된 사례가 확인되었습니다. 같은 날짜에 여러 행사가 함께 열린 사례가 있어, 향후 일정 확정 시 유사한 일정 중복 가능성을 확인할 필요가 있습니다.", count);
        }
        if (count == 1) {
            return String.format("기획한 개최 기간과 동일한 날짜 구간에 과거 주변 행사 %d건이 함께 개최된 사례가 확인되었습니다. 반복적인 일정 집중으로 보기는 어렵지만, 동일 날짜에 행사가 함께 열린 과거 사례가 존재합니다.", count);
        }
        return "기획한 개최 기간과 정확히 동일한 날짜 구간에 주변 행사가 함께 개최된 과거 사례는 확인되지 않았습니다.";
    }

    private String conflictNearbyPeriodSentence(int count) {
        if (count >= 3) {
            return String.format("기획한 개최일 전후의 인접 기간에 과거 주변 행사 %d건이 확인되었습니다. 정확히 같은 날짜는 아니지만 가까운 시기에 행사가 집중된 사례가 있어, 향후 일정 수립 시 주변 행사 일정을 함께 확인할 필요가 있습니다.", count);
        }
        if (count >= 1) {
            return String.format("기획한 개최일 전후의 인접 기간에 과거 주변 행사 %d건이 확인되었습니다. 인접 시기의 행사 집중도가 높은 수준은 아니지만, 일정 수립 시 참고할 수 있는 과거 사례가 존재합니다.", count);
        }
        return "기획한 개최일 전후의 인접 기간에서는 별도의 주변 행사 사례가 확인되지 않았습니다.";
    }

    private String conflictHistoricalSentence(Integer historyYears, int eventYears, BigDecimal rate) {
        if (historyYears == null || rate == null) {
            return "더 넓은 과거 동일 시기 범위의 반복 이력을 해석할 수 있는 데이터가 부족합니다.";
        }
        if (eventYears == 0) {
            return String.format("더 넓은 동일 시기 범위에서도 최근 %d년 동안 주변 행사 사례가 확인되지 않았습니다.", historyYears);
        }
        if (rate.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return String.format("더 넓은 과거 동일 시기 범위로 보면, 최근 %d년 중 %d개 연도에서 주변 행사가 확인되었습니다. 비슷한 시기에 행사가 반복적으로 개최된 이력이 있어 향후 일정 수립 시 해당 패턴을 고려할 필요가 있습니다.", historyYears, eventYears);
        }
        if (rate.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return String.format("더 넓은 과거 동일 시기 범위로 보면, 최근 %d년 중 %d개 연도에서 주변 행사가 확인되었습니다. 반복적인 행사 집중이 뚜렷한 수준은 아니지만, 비슷한 시기에 행사가 개최된 과거 사례가 일부 존재합니다.", historyYears, eventYears);
        }
        return String.format("더 넓은 동일 시기 범위에서는 최근 %d년 중 일부 연도에서만 주변 행사 사례가 확인되었습니다. 과거 기록만으로 해당 시기에 행사가 반복적으로 집중된다고 보기는 어렵습니다.", historyYears);
    }

    private String conflictRegionSentence(Integer count) {
        if (count == null) return "동일 지역 행사 여부를 확인할 수 있는 데이터가 없습니다.";
        if (count >= 2) {
            return String.format("이번 분석에서 확인된 과거 행사 가운데 %d건이 동일 지역에서 개최되었습니다. 향후 비슷한 시기에 지역 내 행사가 함께 열릴 경우 방문객과 교통 수요가 직접적으로 겹칠 가능성을 고려할 필요가 있습니다.", count);
        }
        if (count == 1) {
            return String.format("이번 분석에서 확인된 과거 행사 가운데 %d건이 동일 지역에서 개최된 사례가 있습니다. 반복적인 지역 집중으로 보기는 어렵지만, 향후 일정 확정 시 참고할 수 있는 과거 사례입니다.", count);
        }
        return "이번 분석에서 확인된 과거 행사 가운데 동일 지역에서 개최된 사례는 확인되지 않았습니다.";
    }

    private String conflictThemeSentence(List<ConflictRiskResponse.Event> events) {
        long sameThemeCount = events.stream()
                .filter(event -> Boolean.TRUE.equals(event.sameTheme()))
                .count();
        if (sameThemeCount == 0) return "";
        return String.format("이번 분석에서 확인된 과거 행사 가운데 %d건은 현재 기획과 유사한 테마를 가진 행사입니다. 향후 실제 일정이 겹칠 경우 유사한 방문객층을 두고 수요가 분산될 가능성을 고려할 필요가 있습니다.", sameThemeCount);
    }

    private String conflictSummaryV2(int direct, int nearby, int eventYears, BigDecimal rate) {
        if (direct >= 2) return "과거 동일 날짜에 주변 행사가 함께 개최된 사례가 다수 확인됩니다.";
        if (direct == 1) return "과거 동일 날짜에 주변 행사가 함께 개최된 사례가 확인됩니다.";
        if (nearby >= 3) return "과거 동일 날짜의 직접 중복은 없지만 인접 시기에 행사가 집중된 사례가 확인됩니다.";
        if (rate != null && rate.compareTo(BigDecimal.valueOf(60)) >= 0) return "정확히 같은 날짜의 중복은 없지만 비슷한 시기의 행사 이력이 반복적으로 확인됩니다.";
        if (rate != null && rate.compareTo(BigDecimal.valueOf(20)) >= 0) return "정확히 같은 날짜의 중복 사례는 없지만 비슷한 시기의 과거 행사 이력이 일부 확인됩니다.";
        if (nearby == 0 && eventYears == 0) return "과거 데이터를 기준으로 비슷한 시기의 행사 중복 이력은 확인되지 않았습니다.";
        return "과거 데이터를 기준으로 비슷한 시기의 행사 집중 이력은 제한적인 편입니다.";
    }

    private String conflictConclusionV2(int direct, int nearby, int eventYears, BigDecimal rate) {
        if (direct >= 2) return "현재 미래 일정이 확정적으로 충돌한다고 볼 수는 없지만, 동일 날짜에 행사가 함께 개최된 과거 사례가 다수 존재하므로 향후 일정 확정 과정에서 주변 행사 일정을 확인할 필요가 있습니다.";
        if (direct == 1) return "반복적인 일정 충돌로 단정하기는 어렵지만 동일 날짜에 행사가 함께 개최된 과거 사례가 있으므로, 향후 일정 확정 시 참고할 필요가 있습니다.";
        if (nearby >= 3) return "정확히 같은 날짜에 행사가 겹친 사례는 확인되지 않았지만 개최일 전후에 주변 행사가 집중된 이력이 있어, 향후 일정 수립 시 인접 행사 일정을 함께 고려할 필요가 있습니다.";
        if (rate != null && rate.compareTo(BigDecimal.valueOf(60)) >= 0) return "기획한 날짜와 직접적으로 겹친 과거 사례는 없거나 제한적이지만, 더 넓은 동일 시기 범위에서는 주변 행사가 반복적으로 개최된 이력이 확인됩니다. 향후 일정 수립 시 이러한 반복 패턴을 고려할 필요가 있습니다.";
        if (rate != null && rate.compareTo(BigDecimal.valueOf(20)) >= 0) return "기획한 날짜 및 인접 기간의 직접적인 중복 사례는 제한적이지만, 더 넓은 동일 시기 범위에서는 일부 과거 행사 사례가 확인됩니다. 반복적인 행사 집중 패턴으로 보기는 어려워 현재 단계에서는 참고 수준의 이력으로 해석할 수 있습니다.";
        if (nearby == 0 && eventYears == 0) return "정확히 같은 날짜와 인접 기간뿐 아니라 더 넓은 동일 시기 범위에서도 주변 행사 사례가 확인되지 않아, 현재 확보된 과거 데이터에서는 일정 중복 가능성이 낮은 편으로 볼 수 있습니다.";
        return "기획한 날짜 및 인접 기간에서는 주변 행사 사례가 확인되지 않았으며, 더 넓은 동일 시기 범위에서도 반복적으로 행사가 집중된 패턴은 뚜렷하지 않습니다.";
    }

    private String conflictSummary(int directCount, int nearbyCount, BigDecimal historicalRate) {
        if (directCount >= 2) return "과거 동일 시기에 주변 행사가 함께 개최된 사례가 다수 확인됩니다.";
        if (directCount == 1) return "과거 동일 시기에 주변 행사가 개최된 사례가 확인됩니다.";
        if (nearbyCount >= 3) return "과거 동일 날짜의 직접 중복은 없지만 인접 시기에 행사가 집중된 사례가 확인됩니다.";
        if (historicalRate != null && historicalRate.compareTo(BigDecimal.valueOf(60)) >= 0) return "과거 동일 날짜의 직접 중복은 적지만 비슷한 시기의 행사 집중 이력이 반복적으로 확인됩니다.";
        if (historicalRate != null && historicalRate.compareTo(BigDecimal.valueOf(20)) >= 0) return "과거 동일 시기의 행사 이력은 일부 확인되지만 반복적인 집중 수준은 아닙니다.";
        return "과거 데이터를 기준으로 동일 시기의 행사 집중 가능성은 낮은 편입니다.";
    }

    private String conflictConclusion(int directCount, int nearbyCount, BigDecimal historicalRate) {
        if (directCount >= 2) return "현재 미래 일정이 확정적으로 충돌한다고 볼 수는 없지만, 과거 같은 시기에 행사가 집중된 사례가 있어 향후 일정 확정 과정에서 중복 여부를 확인할 필요가 있습니다.";
        if (directCount == 1) return "반복적인 충돌 패턴으로 단정하기는 어렵지만, 과거 유사한 일정 사례가 존재하므로 향후 개최 일정 확정 시 참고할 필요가 있습니다.";
        if (nearbyCount >= 3) return "정확히 같은 날짜에 행사가 겹친 사례는 확인되지 않았지만, 개최일 전후에 주변 행사가 집중된 이력이 있어 향후 일정 수립 시 중복 가능성을 고려할 필요가 있습니다.";
        if (historicalRate != null && historicalRate.compareTo(BigDecimal.valueOf(60)) >= 0) return "정확히 같은 날짜의 중복 사례는 제한적이지만, 과거 비슷한 시기에 주변 행사가 반복적으로 개최되어 향후에도 일정 중복 가능성이 존재합니다.";
        if (historicalRate != null && historicalRate.compareTo(BigDecimal.valueOf(20)) >= 0) return "과거 일부 유사한 일정 사례가 존재하지만 반복적인 행사 집중 패턴으로 보기는 어려워, 현재 단계에서는 참고 수준의 위험으로 해석할 수 있습니다.";
        return "현재 분석된 과거 이력에서는 동일 날짜나 인접 시기에 주변 행사가 집중된 패턴이 뚜렷하게 나타나지 않았습니다.";
    }

    public InterpretationDecision interpretTrendFit(
            FestivalAnalysis analysis,
            List<FestivalAnalysisTrendKeyword> yearlyRows,
            List<FestivalAnalysisTrendKeyword> monthlyRows) {
        BigDecimal latestIntegratedGrowth = latestIntegratedGrowth(yearlyRows);
        List<KeywordGrowth> keywordGrowths = latestKeywordGrowths(yearlyRows);
        BigDecimal eventPeriodGap = eventPeriodGap(analysis, monthlyRows);

        if (latestIntegratedGrowth == null) {
            return decision("데이터 부족", new ResultInterpretation(
                    "통합 검색 관심도 증감률을 확인할 수 없습니다.",
                    "최근 연도 간 통합 검색 관심도 증감률 데이터가 없어 전체 관심 흐름을 해석하기 어렵습니다."), List.of());
        }

        int keywordCount = keywordGrowths.size();
        List<String> decliningKeywords = keywordGrowths.stream()
                .filter(keyword -> keyword.growthRate().compareTo(BigDecimal.valueOf(-20)) <= 0)
                .map(KeywordGrowth::keyword)
                .toList();
        int decliningKeywordCount = decliningKeywords.size();
        BigDecimal decliningRate = keywordCount == 0 ? null
                : BigDecimal.valueOf(decliningKeywordCount * 100L)
                .divide(BigDecimal.valueOf(keywordCount), 2, RoundingMode.HALF_UP);

        String eventPeriodSentence = eventPeriodSentence(eventPeriodGap);
        String summary = trendSummary(latestIntegratedGrowth, decliningRate);
        String detail = trendDetail(latestIntegratedGrowth, keywordCount, decliningKeywordCount,
                decliningRate, decliningKeywords, eventPeriodSentence);
        return decision(trendStatus(latestIntegratedGrowth), new ResultInterpretation(summary, detail),
                List.of(metric("latestGrowthRate", latestIntegratedGrowth), metric("decliningKeywordRate", decliningRate),
                        metric("eventPeriodGap", eventPeriodGap)));
    }

    private String trendStatus(BigDecimal trend) {
        if (trend.compareTo(BigDecimal.valueOf(20)) >= 0) return "관심 급상승";
        if (trend.compareTo(BigDecimal.valueOf(5)) >= 0) return "관심 상승";
        if (trend.compareTo(BigDecimal.valueOf(-5)) > 0) return "관심 유지";
        if (trend.compareTo(BigDecimal.valueOf(-20)) > 0) return "관심 하락";
        return "관심 급감";
    }

    private BigDecimal latestIntegratedGrowth(List<FestivalAnalysisTrendKeyword> yearlyRows) {
        Map<Integer, List<BigDecimal>> valuesByYear = yearlyRows.stream()
                .filter(row -> row.getPeriodYear() != null && row.getInterestValue() != null)
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getPeriodYear,
                        TreeMap::new, Collectors.mapping(FestivalAnalysisTrendKeyword::getInterestValue, Collectors.toList())));
        List<TrendFitResponse.YearlyInterest> interests = valuesByYear.entrySet().stream()
                .map(entry -> new TrendFitResponse.YearlyInterest(entry.getKey(), average(entry.getValue())))
                .toList();
        List<TrendFitResponse.YearlyGrowthRate> growthRates = growthRates(interests);
        return growthRates.isEmpty() ? null : growthRates.get(growthRates.size() - 1).rate();
    }

    private List<KeywordGrowth> latestKeywordGrowths(List<FestivalAnalysisTrendKeyword> yearlyRows) {
        return yearlyRows.stream()
                .filter(row -> row.getKeyword() != null && !row.getKeyword().isBlank()
                        && row.getPeriodYear() != null && row.getInterestValue() != null)
                .collect(Collectors.groupingBy(FestivalAnalysisTrendKeyword::getKeyword,
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    List<FestivalAnalysisTrendKeyword> rows = entry.getValue().stream()
                            .sorted(Comparator.comparing(FestivalAnalysisTrendKeyword::getPeriodYear))
                            .toList();
                    List<TrendFitResponse.YearlyInterest> interests = rows.stream()
                            .map(row -> new TrendFitResponse.YearlyInterest(row.getPeriodYear(), row.getInterestValue()))
                            .toList();
                    List<TrendFitResponse.YearlyGrowthRate> growthRates = growthRates(interests);
                    if (growthRates.isEmpty()) return null;
                    BigDecimal rate = growthRates.get(growthRates.size() - 1).rate();
                    return rate == null ? null : new KeywordGrowth(entry.getKey(), rate);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private BigDecimal eventPeriodGap(FestivalAnalysis analysis,
                                      List<FestivalAnalysisTrendKeyword> monthlyRows) {
        if (analysis.getFestivalPlan().getStartDate() == null) return null;
        YearMonth eventMonth = YearMonth.from(analysis.getFestivalPlan().getStartDate()).minusYears(1);
        YearMonth start = eventMonth.minusMonths(3);
        YearMonth end = eventMonth.plusMonths(3);
        List<BigDecimal> gaps = new ArrayList<>();
        for (String keyword : monthlyRows.stream()
                .map(FestivalAnalysisTrendKeyword::getKeyword)
                .filter(Objects::nonNull)
                .distinct().toList()) {
            Map<YearMonth, BigDecimal> values = monthlyRows.stream()
                    .filter(row -> keyword.equals(row.getKeyword())
                            && row.getPeriodYear() != null && row.getPeriodMonth() != null)
                    .collect(Collectors.toMap(
                            row -> YearMonth.of(row.getPeriodYear(), row.getPeriodMonth()),
                            FestivalAnalysisTrendKeyword::getInterestValue,
                            (first, second) -> first));
            BigDecimal eventInterest = values.get(eventMonth);
            List<BigDecimal> surrounding = new ArrayList<>();
            for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
                if (!month.equals(eventMonth) && values.get(month) != null) {
                    surrounding.add(values.get(month));
                }
            }
            if (eventInterest == null || surrounding.isEmpty()) continue;
            BigDecimal surroundingAverage = average(surrounding);
            if (surroundingAverage.signum() == 0) continue;
            gaps.add(eventInterest.subtract(surroundingAverage)
                    .divide(surroundingAverage, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        return gaps.isEmpty() ? null : average(gaps).setScale(2, RoundingMode.HALF_UP);
    }

    private String trendSummary(BigDecimal trend, BigDecimal decliningRate) {
        if (trend.compareTo(BigDecimal.valueOf(20)) >= 0) return "축제 핵심 콘텐츠에 대한 검색 관심이 크게 상승하고 있습니다.";
        if (trend.compareTo(BigDecimal.valueOf(5)) >= 0) return "축제 핵심 콘텐츠에 대한 검색 관심이 상승하는 흐름을 보이고 있습니다.";
        if (trend.compareTo(BigDecimal.valueOf(-5)) > 0) return "축제 핵심 콘텐츠에 대한 검색 관심은 전반적으로 안정적인 수준입니다.";
        if (trend.compareTo(BigDecimal.valueOf(-20)) > 0) return "축제 핵심 콘텐츠에 대한 검색 관심이 감소하는 흐름을 보이고 있습니다.";
        if (decliningRate != null && decliningRate.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "축제 핵심 콘텐츠 전반에서 검색 관심이 크게 감소하고 있습니다.";
        }
        return "축제 핵심 콘텐츠에 대한 최근 검색 관심이 크게 감소했습니다.";
    }

    private String trendDetail(BigDecimal trend, int keywordCount, int decliningCount,
                               BigDecimal decliningRate, List<String> decliningKeywords,
                               String eventPeriodSentence) {
        String trendText = formatInterpretation(trend.abs());
        String keywordText = String.format("분석한 %d개 키워드 중 %d개에서 20%% 이상의 관심 감소가 확인되었습니다.",
                keywordCount, decliningCount);
        String detail;
        if (trend.compareTo(BigDecimal.valueOf(20)) >= 0) {
            detail = String.format("핵심 콘텐츠의 통합 검색 관심도는 최근 전년 대비 약 %s%% 증가했습니다. %s 일부 키워드별 차이는 존재하지만, 전체적으로 축제의 핵심 주제와 프로그램에 대한 관심이 뚜렷하게 확대되는 흐름입니다.", trendText, keywordText);
        } else if (trend.compareTo(BigDecimal.valueOf(5)) >= 0) {
            detail = String.format("핵심 콘텐츠의 통합 검색 관심도는 최근 전년 대비 약 %s%% 증가했습니다. %s 개별 콘텐츠별 관심 변화에는 차이가 있지만, 전체적으로 축제의 핵심 주제와 프로그램에 대한 관심이 증가하는 흐름입니다.", trendText, keywordText);
        } else if (trend.compareTo(BigDecimal.valueOf(-5)) > 0) {
            detail = String.format("핵심 콘텐츠의 통합 검색 관심도는 최근 전년 대비 약 %s%% 차이로 큰 변화 없이 유지되고 있습니다. %s 전체 관심 수준은 비교적 안정적이지만, 일부 콘텐츠에서는 개별적인 관심 변화가 나타나고 있습니다.", trendText, keywordText);
        } else if (trend.compareTo(BigDecimal.valueOf(-20)) > 0) {
            detail = String.format("핵심 콘텐츠의 통합 검색 관심도는 최근 전년 대비 약 %s%% 감소했습니다. %s 전체적인 검색 관심이 이전보다 약화되고 있어 현재 콘텐츠가 최근 관광 관심 흐름과 얼마나 부합하는지 함께 살펴볼 필요가 있습니다.", trendText, keywordText);
        } else if (decliningRate != null && decliningRate.compareTo(BigDecimal.valueOf(70)) >= 0) {
            detail = String.format("핵심 콘텐츠의 통합 검색 관심도는 최근 전년 대비 약 %s%% 감소했습니다. 분석한 %d개 키워드 중 %d개(%s%%)에서 20%% 이상의 관심 감소가 확인되어, 일부 키워드가 아닌 콘텐츠 전반에서 관심 약화가 나타나고 있습니다. 특히 %s의 최근 검색 관심 감소가 두드러져 현재 콘텐츠 구성이 최근 관심 흐름과 다소 거리가 있는 것으로 볼 수 있습니다.",
                    trendText, keywordCount, decliningCount, formatInterpretation(decliningRate),
                    String.join(", ", decliningKeywords));
        } else {
            String percentage = decliningRate == null ? "" : formatInterpretation(decliningRate) + "%";
            detail = String.format("핵심 콘텐츠의 통합 검색 관심도는 최근 전년 대비 약 %s%% 감소했습니다. 분석한 %d개 키워드 중 %d개(%s)에서 20%% 이상의 관심 감소가 확인되었습니다. 전체 관심은 크게 약화되었지만 모든 콘텐츠가 동일하게 하락한 것은 아니므로, 키워드별 관심 흐름을 함께 확인할 필요가 있습니다.",
                    trendText, keywordCount, decliningCount, percentage);
        }
        return eventPeriodSentence.isBlank() ? detail : joinInterpretationLines(detail, eventPeriodSentence);
    }

    private String eventPeriodSentence(BigDecimal eventPeriodGap) {
        if (eventPeriodGap == null) return "";
        if (eventPeriodGap.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return String.format("개최 예정 월의 핵심 키워드 검색 관심도는 주변 시기보다 평균 약 %s%% 높게 나타나, 최근 관심 추세와 별개로 해당 시기에 대한 계절적 관심은 뚜렷하게 확인됩니다.", formatInterpretation(eventPeriodGap));
        }
        if (eventPeriodGap.compareTo(BigDecimal.valueOf(-20)) > 0) {
            return String.format("개최 예정 월의 핵심 키워드 검색 관심도는 주변 시기와 약 %s%% 차이로, 개최 시기에 따른 뚜렷한 관심 상승이나 하락은 확인되지 않습니다.", formatInterpretation(eventPeriodGap.abs()));
        }
        return String.format("개최 예정 월의 핵심 키워드 검색 관심도는 주변 시기보다 평균 약 %s%% 낮게 나타나, 현재 개최 시기의 검색 관심도 역시 상대적으로 낮은 수준입니다.", formatInterpretation(eventPeriodGap.abs()));
    }

    private record KeywordGrowth(String keyword, BigDecimal growthRate) {}


    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
    private List<TrendFitResponse.YearlyGrowthRate> growthRates(List<TrendFitResponse.YearlyInterest> values) {
        List<TrendFitResponse.YearlyGrowthRate> result = new ArrayList<>();
        for (int index = 1; index < values.size(); index++) {
            TrendFitResponse.YearlyInterest previous = values.get(index - 1);
            TrendFitResponse.YearlyInterest current = values.get(index);
            result.add(new TrendFitResponse.YearlyGrowthRate(previous.year(), current.year(),
                    calculateGrowth(previous.interest(), current.interest())));
        }
        return result;
    }
    private BigDecimal calculateGrowth(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
    private String valueOrUnavailable(Object value) {
        return value == null ? "확인되지 않음" : String.valueOf(value);
    }
}
