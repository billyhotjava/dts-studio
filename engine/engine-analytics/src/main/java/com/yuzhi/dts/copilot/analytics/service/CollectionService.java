package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsCollection;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsCollectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing analytics collections (folders for organizing cards/dashboards).
 */
@Service
@Transactional
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final AnalyticsCollectionRepository collectionRepository;

    public CollectionService(AnalyticsCollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsCollection> findAll() {
        return collectionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AnalyticsCollection> findById(Long id) {
        return collectionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsCollection> findByParentId(Long parentId) {
        return collectionRepository.findByParentId(parentId);
    }

    public AnalyticsCollection create(AnalyticsCollection collection) {
        collection.setCreatedAt(Instant.now());
        AnalyticsCollection saved = collectionRepository.save(collection);
        log.info("Created analytics collection id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public AnalyticsCollection update(Long id, AnalyticsCollection updated) {
        AnalyticsCollection existing = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setParentId(updated.getParentId());

        return collectionRepository.save(existing);
    }

    public void delete(Long id) {
        collectionRepository.deleteById(id);
        log.info("Deleted analytics collection id={}", id);
    }
}
