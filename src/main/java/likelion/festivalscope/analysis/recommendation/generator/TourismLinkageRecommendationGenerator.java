package likelion.festivalscope.analysis.recommendation.generator;

import likelion.festivalscope.analysis.entity.AnalysisItemType;
import likelion.festivalscope.analysis.entity.FestivalAnalysisItem;
import likelion.festivalscope.analysis.entity.FestivalAnalysisTourismLinkage;
import likelion.festivalscope.analysis.entity.RecommendationPriority;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationContext;
import likelion.festivalscope.analysis.recommendation.dto.RecommendationDraft;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TourismLinkageRecommendationGenerator implements RecommendationGenerator {
    private static final String RECOMMENDATION_TYPE = "TOURISM_LINKAGE";
    private static final int TOURISM_ORDER = 60;
    private static final int COMMERCE_ORDER = 61;
    private static final int STAY_ORDER = 62;

    @Override
    public AnalysisItemType supports() {
        return AnalysisItemType.TOURISM_LINKAGE;
    }

    @Override
    public List<RecommendationDraft> generate(RecommendationContext context) {
        FestivalAnalysisItem item = context.item(supports());
        FestivalAnalysisTourismLinkage tourism = context.tourismLinkage();
        if (item == null || tourism == null) {
            return List.of();
        }

        List<RecommendationDraft> drafts = new ArrayList<>();
        tourismDraft(item, tourism).ifPresent(drafts::add);
        commerceDraft(item, tourism).ifPresent(drafts::add);
        stayDraft(item, tourism).ifPresent(drafts::add);
        return drafts;
    }

    private java.util.Optional<RecommendationDraft> tourismDraft(
            FestivalAnalysisItem item, FestivalAnalysisTourismLinkage tourism) {
        Integer count = tourism.getTourismCultureWithin3kmCount();
        if (count == null || count < 3) {
            return java.util.Optional.empty();
        }

        String content = "행사장 3km 내 연계 가능한 관광·문화자원 " + count + "개가 확인되었습니다. "
                + "축제 전후에 주변 관광지를 함께 방문할 수 있도록 연계 동선이나 관광 안내 요소를 추가해볼 수 있습니다.";
        return java.util.Optional.of(draft(item, "축제 전후 관광 코스를 구성해보세요.", content, TOURISM_ORDER));
    }

    private java.util.Optional<RecommendationDraft> commerceDraft(
            FestivalAnalysisItem item, FestivalAnalysisTourismLinkage tourism) {
        Integer count = tourism.getFoodShoppingWithin3kmCount();
        if (count == null || count < 5) {
            return java.util.Optional.empty();
        }

        String content = "행사장 3km 내 연계 가능한 음식·쇼핑 자원 " + count + "개가 확인되었습니다. "
                + "방문객 전용 쿠폰이나 스탬프 투어 등을 통해 축제 방문을 지역 소비로 연결할 수 있습니다.";
        return java.util.Optional.of(draft(item, "지역 상권 연계 프로그램을 운영해보세요.", content, COMMERCE_ORDER));
    }

    private java.util.Optional<RecommendationDraft> stayDraft(
            FestivalAnalysisItem item, FestivalAnalysisTourismLinkage tourism) {
        Integer count = tourism.getAccommodationWithin5kmCount();
        if (count == null || count < 2) {
            return java.util.Optional.empty();
        }

        String content = "행사장 5km 내 연계 가능한 숙박시설 " + count + "개가 확인되었습니다. "
                + "야간 프로그램이나 숙박 연계 상품을 통해 당일 방문을 체류형 관광으로 확장할 수 있습니다.";
        return java.util.Optional.of(draft(item, "체류형 관광으로의 확장을 검토해보세요.", content, STAY_ORDER));
    }

    private RecommendationDraft draft(FestivalAnalysisItem item, String title,
                                      String content, int displayOrder) {
        return new RecommendationDraft(
                RECOMMENDATION_TYPE,
                RecommendationPriority.OPTIONAL,
                title,
                content,
                displayOrder,
                item);
    }

}
