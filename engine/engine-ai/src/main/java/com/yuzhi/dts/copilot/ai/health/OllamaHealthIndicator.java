package com.yuzhi.dts.copilot.ai.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Ollama 健康检查。仅当 LLM_PROVIDER=ollama 时启用。
 * 默认使用公有云 LLM 时不注册此 HealthIndicator，避免影响整体健康状态。
 */
@Component
@ConditionalOnProperty(name = "dts.copilot.llm.provider", havingValue = "ollama")
public class OllamaHealthIndicator implements HealthIndicator {

    private final String ollamaBaseUrl;
    private final HttpClient httpClient;

    public OllamaHealthIndicator(@Value("${dts.copilot.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Health health() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Health.up()
                        .withDetail("url", ollamaBaseUrl)
                        .build();
            }
            return Health.down()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("statusCode", response.statusCode())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("url", ollamaBaseUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
