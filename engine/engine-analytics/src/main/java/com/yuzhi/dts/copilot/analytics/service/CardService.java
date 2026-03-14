package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsCard;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing analytics cards (visualizations / saved questions).
 */
@Service
@Transactional
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final AnalyticsCardRepository cardRepository;
    private final QueryExecutionService queryExecutionService;

    public CardService(AnalyticsCardRepository cardRepository,
                       QueryExecutionService queryExecutionService) {
        this.cardRepository = cardRepository;
        this.queryExecutionService = queryExecutionService;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsCard> findAll() {
        return cardRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AnalyticsCard> findById(Long id) {
        return cardRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsCard> findByDatabaseId(Long databaseId) {
        return cardRepository.findByDatabaseId(databaseId);
    }

    public AnalyticsCard create(AnalyticsCard card) {
        card.setCreatedAt(Instant.now());
        card.setUpdatedAt(Instant.now());
        AnalyticsCard saved = cardRepository.save(card);
        log.info("Created analytics card id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public AnalyticsCard update(Long id, AnalyticsCard updated) {
        AnalyticsCard existing = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setDatabaseId(updated.getDatabaseId());
        existing.setDatasetQuery(updated.getDatasetQuery());
        existing.setVisualizationSettings(updated.getVisualizationSettings());
        existing.setDisplay(updated.getDisplay());
        existing.setUpdatedAt(Instant.now());

        return cardRepository.save(existing);
    }

    public void delete(Long id) {
        cardRepository.deleteById(id);
        log.info("Deleted analytics card id={}", id);
    }

    /**
     * Execute the query defined in a card and return the results.
     */
    @Transactional(readOnly = true)
    public QueryExecutionService.QueryResult executeCard(Long cardId) {
        AnalyticsCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        if (card.getDatasetQuery() == null || card.getDatasetQuery().isBlank()) {
            throw new IllegalStateException("Card has no dataset query defined");
        }

        return queryExecutionService.executeQuery(card.getDatabaseId(), card.getDatasetQuery());
    }
}
