package likelion.festivalscope.analysis.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisProgressService {
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AnalysisProgressEvent> latestEvents = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 30 * 60 * 1000L;

    public SseEmitter subscribe(Long analysisId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(analysisId, emitter);
        emitter.onCompletion(() -> emitters.remove(analysisId, emitter));
        emitter.onTimeout(() -> emitters.remove(analysisId, emitter));
        emitter.onError(error -> emitters.remove(analysisId, emitter));
        AnalysisProgressEvent latest = latestEvents.get(analysisId);
        if (latest != null) {
            send(analysisId, latest);
            if (latest.status() == AnalysisStepStatus.COMPLETED && "ANALYSIS".equals(latest.step())) {
                emitter.complete();
                emitters.remove(analysisId, emitter);
            }
        }
        return emitter;
    }

    public void send(Long analysisId, AnalysisStep step, AnalysisStepStatus status) {
        String message = status == AnalysisStepStatus.FAILED
                ? step.getLabel() + " 중 오류가 발생했습니다."
                : step.getLabel() + (status == AnalysisStepStatus.COMPLETED ? " 완료" : "");
        send(analysisId, new AnalysisProgressEvent(step.name(), status, message));
    }

    public void sendCompleted(Long analysisId) {
        send(analysisId, new AnalysisProgressEvent("ANALYSIS", AnalysisStepStatus.COMPLETED,
                "축제 기획안 분석이 완료되었습니다."));
    }

    public void sendFailed(Long analysisId) {
        send(analysisId, new AnalysisProgressEvent("ANALYSIS", AnalysisStepStatus.FAILED,
                "축제 기획안 분석에 실패했습니다."));
    }

    private void send(Long analysisId, AnalysisProgressEvent event) {
        latestEvents.put(analysisId, event);
        SseEmitter emitter = emitters.get(analysisId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().name("progress").data(objectMapper.writeValueAsString(event)));
        } catch (Exception exception) {
            emitters.remove(analysisId, emitter);
            log.debug("SSE disconnected for analysis {}", analysisId, exception);
        }
    }

    public void complete(Long analysisId) {
        SseEmitter emitter = emitters.remove(analysisId);
        if (emitter == null) return;
        try { emitter.complete(); } catch (Exception exception) { log.debug("SSE completion failed", exception); }
    }
}
