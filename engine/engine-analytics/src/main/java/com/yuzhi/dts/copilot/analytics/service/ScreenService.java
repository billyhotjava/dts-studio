package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsScreen;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsScreenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing analytics screens (data visualization screens).
 */
@Service
@Transactional
public class ScreenService {

    private static final Logger log = LoggerFactory.getLogger(ScreenService.class);

    private final AnalyticsScreenRepository screenRepository;

    public ScreenService(AnalyticsScreenRepository screenRepository) {
        this.screenRepository = screenRepository;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsScreen> findAll() {
        return screenRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AnalyticsScreen> findById(Long id) {
        return screenRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsScreen> findByStatus(String status) {
        return screenRepository.findByStatus(status);
    }

    public AnalyticsScreen create(AnalyticsScreen screen) {
        screen.setCreatedAt(Instant.now());
        screen.setUpdatedAt(Instant.now());
        AnalyticsScreen saved = screenRepository.save(screen);
        log.info("Created analytics screen id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public AnalyticsScreen update(Long id, AnalyticsScreen updated) {
        AnalyticsScreen existing = screenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setConfig(updated.getConfig());
        existing.setStatus(updated.getStatus());
        existing.setUpdatedAt(Instant.now());

        return screenRepository.save(existing);
    }

    public void delete(Long id) {
        screenRepository.deleteById(id);
        log.info("Deleted analytics screen id={}", id);
    }
}
