package likelion.festivalscope.analysis.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "festival_analysis_weather_risk_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalAnalysisWeatherRiskSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weather_risk_snapshot_id")
    private Long weatherRiskSnapshotId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "festival_analysis_item_id", nullable = false, unique = true)
    private FestivalAnalysisItem festivalAnalysisItem;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;
}
