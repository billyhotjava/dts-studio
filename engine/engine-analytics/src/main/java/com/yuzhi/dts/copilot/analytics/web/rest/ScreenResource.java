package com.yuzhi.dts.copilot.analytics.web.rest;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsScreen;
import com.yuzhi.dts.copilot.analytics.service.ScreenService;
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
 * REST controller for analytics screen management.
 */
@RestController
@RequestMapping("/api/screens")
public class ScreenResource {

    private final ScreenService screenService;

    public ScreenResource(ScreenService screenService) {
        this.screenService = screenService;
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsScreen>> list() {
        return ResponseEntity.ok(screenService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalyticsScreen> get(@PathVariable Long id) {
        return screenService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AnalyticsScreen> create(@RequestBody AnalyticsScreen screen) {
        AnalyticsScreen created = screenService.create(screen);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnalyticsScreen> update(@PathVariable Long id,
                                                  @RequestBody AnalyticsScreen screen) {
        try {
            AnalyticsScreen updated = screenService.update(id, screen);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        screenService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
