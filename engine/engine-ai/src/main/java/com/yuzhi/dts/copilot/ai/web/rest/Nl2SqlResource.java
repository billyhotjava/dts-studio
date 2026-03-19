package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.AiChatFeedback;
import com.yuzhi.dts.copilot.ai.repository.AiChatFeedbackRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.SuggestedQuestion;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for NL2SQL template operations.
 */
@RestController
@RequestMapping("/api/ai/nl2sql")
public class Nl2SqlResource {

    private static final Logger log = LoggerFactory.getLogger(Nl2SqlResource.class);

    private final TemplateMatcherService templateMatcherService;
    private final AiChatFeedbackRepository feedbackRepository;

    public Nl2SqlResource(TemplateMatcherService templateMatcherService,
                          AiChatFeedbackRepository feedbackRepository) {
        this.templateMatcherService = templateMatcherService;
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * GET /api/ai/nl2sql/suggestions : Get suggested questions for the welcome card.
     *
     * @param limit maximum number of suggestions (default 12)
     * @return list of suggested questions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<SuggestedQuestion>>> getSuggestions(
            @RequestParam(defaultValue = "12") int limit) {
        List<SuggestedQuestion> suggestions = templateMatcherService.getSuggestedQuestions(limit);
        return ResponseEntity.ok(ApiResponse.ok(suggestions));
    }

    /**
     * POST /api/ai/nl2sql/feedback : Save user feedback on NL2SQL results.
     *
     * @param body feedback payload
     * @return 200 OK
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Void>> saveFeedback(@RequestBody Map<String, Object> body) {
        try {
            AiChatFeedback feedback = new AiChatFeedback();
            feedback.setSessionId(getString(body, "sessionId"));
            feedback.setMessageId(getString(body, "messageId"));
            feedback.setRating(getString(body, "rating"));
            feedback.setReason(getString(body, "reason"));
            feedback.setDetail(getString(body, "detail"));
            feedback.setGeneratedSql(getString(body, "generatedSql"));
            feedback.setCorrectedSql(getString(body, "correctedSql"));
            feedback.setRoutedDomain(getString(body, "routedDomain"));
            feedback.setTargetView(getString(body, "targetView"));
            feedback.setTemplateCode(getString(body, "templateCode"));

            feedbackRepository.save(feedback);
            log.debug("Saved NL2SQL feedback for session={}, rating={}",
                    feedback.getSessionId(), feedback.getRating());

            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            log.error("Failed to save feedback", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to save feedback: " + e.getMessage()));
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
