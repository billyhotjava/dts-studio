package com.yuzhi.dts.copilot.analytics.web.rest;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsCard;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboard;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboardCard;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsScreen;
import com.yuzhi.dts.copilot.analytics.service.CardService;
import com.yuzhi.dts.copilot.analytics.service.DashboardService;
import com.yuzhi.dts.copilot.analytics.service.ScreenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public endpoints for shared cards, dashboards, and screens.
 * These endpoints do not require authentication.
 */
@RestController
@RequestMapping("/api/public")
public class PublicResource {

    private final CardService cardService;
    private final DashboardService dashboardService;
    private final ScreenService screenService;

    public PublicResource(CardService cardService,
                          DashboardService dashboardService,
                          ScreenService screenService) {
        this.cardService = cardService;
        this.dashboardService = dashboardService;
        this.screenService = screenService;
    }

    @GetMapping("/card/{id}")
    public ResponseEntity<AnalyticsCard> getCard(@PathVariable Long id) {
        return cardService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard/{id}")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable Long id) {
        return dashboardService.findById(id)
                .map(dashboard -> {
                    List<AnalyticsDashboardCard> cards = dashboardService.getCards(id);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("dashboard", dashboard);
                    body.put("cards", cards);
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/screen/{id}")
    public ResponseEntity<AnalyticsScreen> getScreen(@PathVariable Long id) {
        return screenService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
