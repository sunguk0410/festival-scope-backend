package likelion.festivalscope.analysis.orchestration;

import likelion.festivalscope.analysis.service.FestivalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalAnalysisOrchestrator {
    private final FestivalAnalysisService analysisService;
    private final AnalysisProgressService progressService;

    @Async("analysisTaskExecutor")
    public void start(Long analysisId) {
        try {
            executeStep(analysisId, AnalysisStep.SIMILAR_FESTIVAL, () -> analysisService.runSimilarFestival(analysisId));
            executeStep(analysisId, AnalysisStep.TREND_FIT, () -> analysisService.runTrendFit(analysisId));
            executeStep(analysisId, AnalysisStep.DEMAND_FIT, () -> analysisService.runDemandFit(analysisId));
            executeStep(analysisId, AnalysisStep.COMPETITION_RISK, () -> analysisService.runCompetitionRisk(analysisId));
            executeStep(analysisId, AnalysisStep.WEATHER_RISK, () -> analysisService.runWeatherRisk(analysisId));
            executeStep(analysisId, AnalysisStep.TOURISM_LINKAGE, () -> analysisService.runTourismLinkage(analysisId));
            executeStep(analysisId, AnalysisStep.FINALIZING, () -> analysisService.finalizeAnalysis(analysisId));
            analysisService.markCompleted(analysisId);
            progressService.sendCompleted(analysisId);
        } catch (Exception exception) {
            analysisService.markFailed(analysisId);
            progressService.sendFailed(analysisId);
            log.error("Festival analysis failed: {}", analysisId, exception);
        } finally {
            progressService.complete(analysisId);
        }
    }

    private void executeStep(Long analysisId, AnalysisStep step, Runnable task) {
        progressService.send(analysisId, step, AnalysisStepStatus.RUNNING);
        try {
            task.run();
            progressService.send(analysisId, step, AnalysisStepStatus.COMPLETED);
        } catch (Exception exception) {
            progressService.send(analysisId, step, AnalysisStepStatus.FAILED);
            throw exception;
        }
    }
}
