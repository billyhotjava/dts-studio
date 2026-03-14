package com.yuzhi.dts.copilot.analytics.web.rest;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboard;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboardCard;
import com.yuzhi.dts.copilot.analytics.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for analytics dashboard management.
 */
@RestController
@RequestMapping("/api/dashboards")
public class DashboardResource {

    private final DashboardService dashboardService;

    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsDashboard>> list() {
        return ResponseEntity.ok(dashboardService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalyticsDashboard> get(@PathVariable Long id) {
        return dashboardService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AnalyticsDashboard> create(@RequestBody AnalyticsDashboard dashboard) {
        AnalyticsDashboard created = dashboardService.create(dashboard);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnalyticsDashboard> update(@PathVariable Long id,
                                                     @RequestBody AnalyticsDashboard dashboard) {
        try {
            AnalyticsDashboard updated = dashboardService.update(id, dashboard);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dashboardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Dashboard Card endpoints ---

    @GetMapping("/{id}/cards")
    public ResponseEntity<List<AnalyticsDashboardCard>> listCards(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getCards(id));
    }

    @PostMapping("/{id}/cards")
    public ResponseEntity<AnalyticsDashboardCard> addCard(@PathVariable Long id,
                                                          @RequestBody AnalyticsDashboardCard dashboardCard) {
        dashboardCard.setDashboardId(id);
        AnalyticsDashboardCard created = dashboardService.addCard(dashboardCard);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{dashboardId}/cards/{cardId}")
    public ResponseEntity<AnalyticsDashboardCard> updateCard(@PathVariable Long dashboardId,
                                                              @PathVariable Long cardId,
                                                              @RequestBody AnalyticsDashboardCard dashboardCard) {
        try {
            AnalyticsDashboardCard updated = dashboardService.updateCard(cardId, dashboardCard);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{dashboardId}/cards/{cardId}")
    public ResponseEntity<Void> removeCard(@PathVariable Long dashboardId,
                                           @PathVariable Long cardId) {
        dashboardService.removeCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
