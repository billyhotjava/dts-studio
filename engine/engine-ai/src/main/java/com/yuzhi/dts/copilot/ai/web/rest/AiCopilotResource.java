package com.yuzhi.dts.copilot.ai.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.copilot.AiCopilotService;
import com.yuzhi.dts.copilot.ai.service.copilot.Nl2SqlService;
import com.yuzhi.dts.copilot.ai.service.copilot.ScreenGenerationService;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import com.yuzhi.dts.copilot.ai.web.rest.dto.CopilotRequest;
import com.yuzhi.dts.copilot.ai.web.rest.dto.Nl2SqlRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AI Copilot operations.
 */
@RestController
@RequestMapping("/api/ai/copilot")
public class AiCopilotResource {

    private static final Logger log = LoggerFactory.getLogger(AiCopilotResource.class);

    private final AiCopilotService copilotService;
    private final Nl2SqlService nl2SqlService;
    private final LlmGatewayService llmGatewayService;
    private final ScreenGenerationService screenGenerationService;

    public AiCopilotResource(AiCopilotService copilotService,
                             Nl2SqlService nl2SqlService,
                             LlmGatewayService llmGatewayService,
                             ScreenGenerationService screenGenerationService) {
        this.copilotService = copilotService;
        this.nl2SqlService = nl2SqlService;
        this.llmGatewayService = llmGatewayService;
        this.screenGenerationService = screenGenerationService;
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<String>> complete(@Valid @RequestBody CopilotRequest request) {
        try {
            String result = copilotService.complete(request.prompt(), request.context());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            log.error("Completion failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Completion failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void completeStream(@Valid @RequestBody CopilotRequest request,
                               HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        try {
            copilotService.completeStream(request.prompt(), request.context(),
                    response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("Streaming completion failed", e);
            try {
                response.getOutputStream().write(
                        ("data: {\"error\":\"" + e.getMessage() + "\"}\n\n").getBytes());
                response.getOutputStream().flush();
            } catch (IOException ignored) {
                // Client disconnected
            }
        }
    }

    @PostMapping("/nl2sql")
    public ResponseEntity<ApiResponse<String>> nl2sql(@Valid @RequestBody Nl2SqlRequest request) {
        try {
            String sql = nl2SqlService.nl2sql(request.naturalLanguage(), request.schemaContext());
            return ResponseEntity.ok(ApiResponse.ok(sql));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            log.error("NL2SQL failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("NL2SQL failed: " + e.getMessage()));
        }
    }

    @PostMapping("/explain")
    public ResponseEntity<ApiResponse<String>> explain(@Valid @RequestBody CopilotRequest request) {
        try {
            String result = copilotService.explain(request.prompt());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            log.error("SQL explain failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Explain failed: " + e.getMessage()));
        }
    }

    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<String>> optimize(@Valid @RequestBody CopilotRequest request) {
        try {
            String result = copilotService.optimize(request.prompt());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            log.error("SQL optimize failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Optimize failed: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/screen/generate")
    public ResponseEntity<ApiResponse<ObjectNode>> generateScreen(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("prompt is required"));
        }
        int width = request.get("width") instanceof Number n ? n.intValue() : 1920;
        int height = request.get("height") instanceof Number n ? n.intValue() : 1080;
        try {
            ObjectNode result = screenGenerationService.generate(prompt, width, height);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            log.error("Screen generation failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Screen generation failed: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/screen/revise")
    public ResponseEntity<ApiResponse<ObjectNode>> reviseScreen(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("prompt is required"));
        }
        JsonNode screenSpec = request.get("screenSpec") instanceof Map<?, ?> m
                ? new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(m)
                : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        List<String> context = request.get("context") instanceof List<?> list
                ? list.stream().map(Object::toString).toList()
                : List.of();
        String mode = request.get("mode") instanceof String s ? s : "apply";
        try {
            ObjectNode result = screenGenerationService.revise(prompt, screenSpec, context, mode);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IOException e) {
            log.error("Screen revision failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Screen revision failed: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        List<String> availableProviders = llmGatewayService.getAvailableProviders();
        Map<String, Object> statusMap = new LinkedHashMap<>();
        statusMap.put("available", !availableProviders.isEmpty());
        statusMap.put("providers", availableProviders);
        return ResponseEntity.ok(ApiResponse.ok(statusMap));
    }
}
