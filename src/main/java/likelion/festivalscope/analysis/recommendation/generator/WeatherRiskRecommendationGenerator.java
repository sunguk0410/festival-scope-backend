package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import likelion.festivalscope.analysis.weather.dto.WeatherRiskResponse;
import likelion.festivalscope.plan.entity.VenueType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class WeatherRiskRecommendationGenerator implements RecommendationGenerator {
    private static final String RECOMMENDATION_TYPE = "WEATHER_RISK";
    private static final int RAIN_ORDER = 50;
    private static final int HOT_ORDER = 51;
    private static final int WIND_ORDER = 52;

    @Override
    public AnalysisItemType supports() {
        return AnalysisItemType.WEATHER_RISK;
    }

    @Override
    public List<RecommendationDraft> generate(RecommendationContext context) {
        FestivalAnalysisItem item = context.item(supports());
        WeatherRiskResponse weather = context.weatherRisk();
        if (item == null || weather == null || weather.analysisPeriod() == null
                || weather.analysisPeriod().actualYears() <= 0
                || weather.festivalCondition() == null
                || weather.festivalCondition().spaceType() == null) {
            return List.of();
        }

        List<RecommendationDraft> drafts = new ArrayList<>();
        rainDraft(item, weather).ifPresent(drafts::add);
        hotDraft(item, weather).ifPresent(drafts::add);
        windDraft(item, weather).ifPresent(drafts::add);
        return drafts;
    }

    private java.util.Optional<RecommendationDraft> rainDraft(
            FestivalAnalysisItem item, WeatherRiskResponse weather) {
        WeatherRiskResponse.Rain rain = weather.rain();
        if (rain == null || rain.occurrenceRate() == null) {
            return java.util.Optional.empty();
        }
        RecommendationPriority priority = priorityFor(rain.occurrenceRate());
        if (priority == null) {
            return java.util.Optional.empty();
        }

        int actualYears = weather.analysisPeriod().actualYears();
        String rate = format(rain.occurrenceRate());
        String title;
        String content;
        if (weather.festivalCondition().spaceType() == VenueType.OUTDOOR) {
            title = switch (priority) {
                case IMMEDIATE -> "우천 대응 계획을 우선 보완하세요.";
                case REVIEW -> "우천 대응 계획을 검토하세요.";
                case OPTIONAL -> "기본적인 우천 대응 방안을 준비하세요.";
            };
            content = switch (priority) {
                case IMMEDIATE -> String.format("최근 %d년 중 동일 시기에 강수가 발생한 해는 %d개년(%s%%)입니다. "
                                + "야외 행사 특성상 강수 시 프로그램 운영 차질 가능성이 높습니다. "
                                + "실내 대체 공간이나 우천 시 대체 프로그램을 사전에 마련하는 것을 권장합니다.",
                        actualYears, rain.occurrenceYears(), rate);
                case REVIEW -> String.format("최근 %d년 중 동일 시기에 강수가 발생한 해는 %d개년(%s%%)입니다. "
                                + "야외 프로그램 운영에 영향을 줄 수 있으므로 우천 시 대체 운영 방안을 준비하는 것을 권장합니다.",
                        actualYears, rain.occurrenceYears(), rate);
                case OPTIONAL -> String.format("최근 %d년 중 동일 시기에 강수가 발생한 해는 %d개년(%s%%)입니다. "
                                + "야외 프로그램 운영에 영향을 줄 수 있으므로 기본적인 우천 대응 계획을 사전에 점검하는 것을 권장합니다.",
                        actualYears, rain.occurrenceYears(), rate);
            };
        } else if (weather.festivalCondition().spaceType() == VenueType.MIXED) {
            title = priority == RecommendationPriority.IMMEDIATE
                    ? "야외 프로그램의 우천 대응 계획을 검토하세요."
                    : priority == RecommendationPriority.REVIEW
                    ? "야외 프로그램의 우천 대응 방안을 점검하세요."
                    : "야외 프로그램의 기본 우천 대응을 점검하세요.";
            content = priority == RecommendationPriority.IMMEDIATE
                    ? String.format("최근 %d년 중 동일 시기에 강수가 발생한 해는 %d개년(%s%%)입니다. "
                            + "야외 프로그램의 실내 전환이나 대체 운영 방안을 준비하는 것을 권장합니다.",
                    actualYears, rain.occurrenceYears(), rate)
                    : String.format("최근 %d년 중 동일 시기에 강수가 발생한 해는 %d개년(%s%%)입니다. "
                            + "야외 프로그램 운영에 영향을 줄 수 있으므로 기본적인 우천 대응 계획을 사전에 점검하는 것을 권장합니다.",
                    actualYears, rain.occurrenceYears(), rate);
        } else {
            title = priority == RecommendationPriority.IMMEDIATE
                    ? "우천 시 이동 동선을 점검하세요."
                    : priority == RecommendationPriority.REVIEW
                    ? "우천 시 외부 이동 대응을 검토하세요."
                    : "우천 시 외부 이동 대응을 점검하세요.";
            content = String.format("최근 %d년 중 동일 시기에 강수가 발생한 해는 %d개년(%s%%)입니다. "
                            + "실내 행사이므로 프로그램 영향은 제한적이지만 입장 대기, 주차, 행사장 이동 동선의 불편 가능성을 점검하는 것을 권장합니다.",
                    actualYears, rain.occurrenceYears(), rate);
        }
        return java.util.Optional.of(draft(item, priority, title, content, RAIN_ORDER));
    }

    private java.util.Optional<RecommendationDraft> hotDraft(
            FestivalAnalysisItem item, WeatherRiskResponse weather) {
        WeatherRiskResponse.Temperature temperature = weather.temperature();
        if (temperature == null || temperature.hotOccurrenceRate() == null) {
            return java.util.Optional.empty();
        }
        RecommendationPriority priority = priorityFor(temperature.hotOccurrenceRate());
        if (priority == null) {
            return java.util.Optional.empty();
        }

        int actualYears = weather.analysisPeriod().actualYears();
        String rate = format(temperature.hotOccurrenceRate());
        VenueType spaceType = weather.festivalCondition().spaceType();
        String title;
        String content;
        if (spaceType == VenueType.OUTDOOR) {
            title = switch (priority) {
                case IMMEDIATE -> "고온 대응 운영계획을 우선 보완하세요.";
                case REVIEW -> "고온 대응 계획을 검토하세요.";
                case OPTIONAL -> "고온 대응 환경을 사전에 점검하세요.";
            };
            content = switch (priority) {
                case IMMEDIATE -> {
                    if (temperature.averageMaxTemperature() == null) yield null;
                    yield String.format("최근 %d년 중 동일 시기에 고온이 발생한 해는 %d개년(%s%%)이며, 평균 최고기온은 %s℃입니다. "
                                    + "장시간 야외 체류에 대비해 그늘·휴식공간, 음수 제공, 운영시간 조정 등의 대책을 마련하는 것을 권장합니다.",
                            actualYears, temperature.hotOccurrenceYears(), rate,
                            format(temperature.averageMaxTemperature()));
                }
                case REVIEW -> String.format("최근 %d년 중 동일 시기에 고온이 발생한 해는 %d개년(%s%%)입니다. "
                                + "야외 체류시간을 고려해 휴식공간과 운영시간 등 방문객 보호 대책을 보완하는 것을 권장합니다.",
                        actualYears, temperature.hotOccurrenceYears(), rate);
                case OPTIONAL -> String.format("최근 %d년 중 동일 시기에 고온이 발생한 해는 %d개년(%s%%)입니다. "
                                + "야외 행사인 만큼 그늘·휴식공간과 음수 제공 계획을 사전에 점검해두는 것을 권장합니다.",
                        actualYears, temperature.hotOccurrenceYears(), rate);
            };
        } else if (spaceType == VenueType.MIXED) {
            title = priority == RecommendationPriority.IMMEDIATE
                    ? "야외 프로그램의 고온 대응 계획을 우선 보완하세요."
                    : priority == RecommendationPriority.REVIEW
                    ? "야외 프로그램의 고온 대응 계획을 검토하세요."
                    : "야외 프로그램의 고온 대응 환경을 점검하세요.";
            content = priority == RecommendationPriority.IMMEDIATE
                    ? String.format("최근 %d년 중 동일 시기에 고온이 발생한 해는 %d개년(%s%%)입니다. "
                            + "실내·외 혼합 행사 중 야외 프로그램과 이동 구간이 고온에 직접 노출될 수 있습니다. "
                            + "운영시간 조정, 그늘·휴식공간, 음수 제공 등 야외 구간 중심의 대응 계획을 우선 보완하는 것을 권장합니다.",
                    actualYears, temperature.hotOccurrenceYears(), rate)
                    : String.format("최근 %d년 중 동일 시기에 고온이 발생한 해는 %d개년(%s%%)입니다. "
                            + "실내·외 혼합 행사인 만큼 야외 프로그램과 이동 구간을 중심으로 그늘·휴식공간, 음수 제공 계획을 점검하는 것을 권장합니다.",
                    actualYears, temperature.hotOccurrenceYears(), rate);
        } else {
            title = priority == RecommendationPriority.IMMEDIATE
                    ? "입장 대기 및 외부 이동 구간의 고온 대응을 우선 보완하세요."
                    : priority == RecommendationPriority.REVIEW
                    ? "입장 대기 및 외부 이동 구간의 고온 대응을 검토하세요."
                    : "입장 대기 및 외부 이동 구간의 고온 대응을 점검하세요.";
            content = String.format("최근 %d년 중 동일 시기에 고온이 발생한 해는 %d개년(%s%%)입니다. "
                            + "실내 프로그램에 대한 직접적인 영향은 제한적이지만, 입장 대기열과 주차장·행사장 간 이동 과정에서 고온 노출이 발생할 수 있습니다. "
                            + "외부 대기시간 단축, 그늘·음수 제공 등 이동·대기 구간의 보호 대책을 마련하는 것을 권장합니다.",
                    actualYears, temperature.hotOccurrenceYears(), rate);
        }
        if (content == null) return java.util.Optional.empty();
        return java.util.Optional.of(draft(item, priority, title, content, HOT_ORDER));
    }

    private java.util.Optional<RecommendationDraft> windDraft(
            FestivalAnalysisItem item, WeatherRiskResponse weather) {
        WeatherRiskResponse.Wind wind = weather.wind();
        if (wind == null || wind.strongWindOccurrenceRate() == null) {
            return java.util.Optional.empty();
        }
        RecommendationPriority priority = priorityFor(wind.strongWindOccurrenceRate());
        if (priority == null) {
            return java.util.Optional.empty();
        }

        int actualYears = weather.analysisPeriod().actualYears();
        String rate = format(wind.strongWindOccurrenceRate());
        VenueType spaceType = weather.festivalCondition().spaceType();
        String title;
        String content;
        if (spaceType == VenueType.OUTDOOR) {
            title = switch (priority) {
                case IMMEDIATE -> "강풍 대응 시설계획을 우선 보완하세요.";
                case REVIEW -> "야외 시설물의 강풍 대응 계획을 검토하세요.";
                case OPTIONAL -> "강풍 대응 기준을 사전에 점검하세요.";
            };
            content = switch (priority) {
                case IMMEDIATE -> "무대·천막·배너 등 야외 시설물 안전에 직접적인 영향을 줄 수 있으므로 "
                        + "시설물 고정 기준과 운영 중단 기준을 사전에 마련하는 것을 권장합니다.";
                case REVIEW -> "무대와 천막 등 야외 시설물의 안전 점검과 운영 기준을 보완하는 것을 권장합니다.";
                case OPTIONAL -> "야외 시설물 운영을 고려해 무대·천막·배너 등의 고정 상태와 기본적인 안전 점검 기준을 사전에 확인하는 것을 권장합니다.";
            };
        } else if (spaceType == VenueType.MIXED) {
            title = priority == RecommendationPriority.IMMEDIATE
                    ? "야외 시설물의 강풍 대응 계획을 우선 보완하세요."
                    : priority == RecommendationPriority.REVIEW
                    ? "야외 시설물의 강풍 대응 기준을 검토하세요."
                    : "야외 시설물의 강풍 대응 기준을 점검하세요.";
            content = priority == RecommendationPriority.IMMEDIATE
                    ? "실내·외 혼합 행사 중 야외 무대·천막·안내시설은 강풍에 직접 노출될 수 있습니다. "
                    + "시설물 고정 기준과 운영 중단 기준을 사전에 마련하고, 야외 프로그램 운영계획을 우선 보완하는 것을 권장합니다."
                    : "실외 구간에 설치되는 무대·천막·배너 등의 고정 상태와 운영 중단 기준을 점검하는 것을 권장합니다.";
        } else {
            title = priority == RecommendationPriority.REVIEW
                    ? "외부 부대시설의 강풍 안전을 검토하세요."
                    : priority == RecommendationPriority.IMMEDIATE
                    ? "외부 부대시설의 강풍 안전을 우선 보완하세요."
                    : "외부 부대시설의 강풍 안전을 점검하세요.";
            content = "실내 프로그램에 대한 직접적인 영향은 제한적이지만, 외부 안내물·대기공간·임시 천막 등 부대시설은 강풍 영향을 받을 수 있습니다. "
                    + "외부 시설물의 고정 상태와 안전 기준을 점검하는 것을 권장합니다.";
        }
        content = String.format("최근 %d년 중 동일 시기에 강풍이 발생한 해는 %d개년(%s%%)입니다. %s",
                actualYears, wind.strongWindOccurrenceYears(), rate, content);
        return java.util.Optional.of(draft(item, priority, title, content, WIND_ORDER));
    }

    private RecommendationPriority priorityFor(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.valueOf(60)) >= 0) return RecommendationPriority.IMMEDIATE;
        if (rate.compareTo(BigDecimal.valueOf(40)) >= 0) return RecommendationPriority.REVIEW;
        if (rate.compareTo(BigDecimal.valueOf(20)) >= 0) return RecommendationPriority.OPTIONAL;
        return null;
    }

    private RecommendationDraft draft(FestivalAnalysisItem item, RecommendationPriority priority,
                                      String title, String content, int displayOrder) {
        return new RecommendationDraft(RECOMMENDATION_TYPE, priority, title, content, displayOrder, item);
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
