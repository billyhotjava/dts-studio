package com.yuzhi.dts.copilot.ai.service.rag;

import com.yuzhi.dts.copilot.ai.service.rag.dto.RagResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Main entry point for RAG (Retrieval-Augmented Generation) operations.
 * Used by the AI copilot service for context enrichment.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int DEFAULT_LIMIT = 5;

    private final HybridSearchService hybridSearchService;
    private final VectorStoreService vectorStoreService;

    public RagService(HybridSearchService hybridSearchService,
                      VectorStoreService vectorStoreService) {
        this.hybridSearchService = hybridSearchService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Retrieve relevant context for the given query using hybrid search.
     *
     * @param query the user's query
     * @param limit maximum number of results
     * @return ranked list of relevant content chunks
     */
    public List<RagResult> retrieve(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        try {
            return hybridSearchService.search(query, effectiveLimit);
        } catch (Exception e) {
            log.error("RAG retrieval failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieve relevant context with the default limit.
     */
    public List<RagResult> retrieve(String query) {
        return retrieve(query, DEFAULT_LIMIT);
    }

    /**
     * Index new content for later retrieval.
     *
     * @param contentType type of content (e.g. "schema", "document", "sql")
     * @param sourceId    unique source identifier
     * @param text        the text content to index
     * @param metadata    optional metadata
     */
    public void index(String contentType, String sourceId, String text, Map<String, String> metadata) {
        if (text == null || text.isBlank()) {
            log.warn("Skipping index for empty text: type={}, sourceId={}", contentType, sourceId);
            return;
        }
        vectorStoreService.insert(contentType, sourceId, text, metadata);
        log.info("Indexed content: type={}, sourceId={}", contentType, sourceId);
    }

    /**
     * Remove indexed content.
     */
    public void removeIndex(String contentType, String sourceId) {
        vectorStoreService.delete(contentType, sourceId);
        log.info("Removed index: type={}, sourceId={}", contentType, sourceId);
    }
}
