package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboard;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboardCard;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDashboardCardRepository;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDashboardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing analytics dashboards and their card layouts.
 */
@Service
@Transactional
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final AnalyticsDashboardRepository dashboardRepository;
    private final AnalyticsDashboardCardRepository dashboardCardRepository;

    public DashboardService(AnalyticsDashboardRepository dashboardRepository,
                            AnalyticsDashboardCardRepository dashboardCardRepository) {
        this.dashboardRepository = dashboardRepository;
        this.dashboardCardRepository = dashboardCardRepository;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDashboard> findAll() {
        return dashboardRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AnalyticsDashboard> findById(Long id) {
        return dashboardRepository.findById(id);
    }

    public AnalyticsDashboard create(AnalyticsDashboard dashboard) {
        dashboard.setCreatedAt(Instant.now());
        dashboard.setUpdatedAt(Instant.now());
        AnalyticsDashboard saved = dashboardRepository.save(dashboard);
        log.info("Created analytics dashboard id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public AnalyticsDashboard update(Long id, AnalyticsDashboard updated) {
        AnalyticsDashboard existing = dashboardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setUpdatedAt(Instant.now());

        return dashboardRepository.save(existing);
    }

    public void delete(Long id) {
        dashboardCardRepository.deleteByDashboardId(id);
        dashboardRepository.deleteById(id);
        log.info("Deleted analytics dashboard id={}", id);
    }

    // --- Dashboard Card management ---

    @Transactional(readOnly = true)
    public List<AnalyticsDashboardCard> getCards(Long dashboardId) {
        return dashboardCardRepository.findByDashboardId(dashboardId);
    }

    public AnalyticsDashboardCard addCard(AnalyticsDashboardCard dashboardCard) {
        return dashboardCardRepository.save(dashboardCard);
    }

    public AnalyticsDashboardCard updateCard(Long dashCardId, AnalyticsDashboardCard updated) {
        AnalyticsDashboardCard existing = dashboardCardRepository.findById(dashCardId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard card not found: " + dashCardId));

        existing.setCardId(updated.getCardId());
        existing.setRow(updated.getRow());
        existing.setCol(updated.getCol());
        existing.setSizeX(updated.getSizeX());
        existing.setSizeY(updated.getSizeY());

        return dashboardCardRepository.save(existing);
    }

    public void removeCard(Long dashCardId) {
        dashboardCardRepository.deleteById(dashCardId);
    }
}
