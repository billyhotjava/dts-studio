package com.yuzhi.dts.copilot.ai.service.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.RagEmbedding;
import com.yuzhi.dts.copilot.ai.repository.RagEmbeddingRepository;
import com.yuzhi.dts.copilot.ai.service.rag.dto.RagResult;
import com.yuzhi.dts.copilot.ai.service.rag.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hybrid search combining vector similarity (pgvector) and keyword search (tsvector/tsquery)
 * using Reciprocal Rank Fusion (RRF) for result merging.
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int RRF_K = 60;

    private final EmbeddingService embeddingService;
    private final RagEmbeddingRepository ragEmbeddingRepository;

    public HybridSearchService(EmbeddingService embeddingService,
                               RagEmbeddingRepository ragEmbeddingRepository) {
        this.embeddingService = embeddingService;
        this.ragEmbeddingRepository = ragEmbeddingRepository;
    }

    /**
     * Perform hybrid search combining vector and keyword results with RRF fusion.
     *
     * @param query the user's search query
     * @param limit maximum number of results to return
     * @return ranked list of RAG results
     */
    public List<RagResult> search(String query, int limit) {
        int fetchLimit = limit * 3; // fetch more candidates for RRF

        // Vector search
        List<RagEmbedding> vectorResults = performVectorSearch(query, fetchLimit);

        // Keyword search
        List<RagEmbedding> keywordResults = performKeywordSearch(query, fetchLimit);

        // RRF fusion
        return fuseWithRRF(vectorResults, keywordResults, limit);
    }

    private List<RagEmbedding> performVectorSearch(String query, int limit) {
        try {
            float[] queryEmbedding = embeddingService.embed(query);
            if (queryEmbedding == null) {
                log.warn("Embedding service unavailable, skipping vector search");
                return List.of();
            }
            String vectorLiteral = toVectorLiteral(queryEmbedding);
            return ragEmbeddingRepository.searchByVector(vectorLiteral, limit);
        } catch (Exception e) {
            log.warn("Vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RagEmbedding> performKeywordSearch(String query, int limit) {
        try {
            String tsQuery = toTsQuery(query);
            if (tsQuery.isBlank()) {
                return List.of();
            }
            return ragEmbeddingRepository.searchByKeyword(tsQuery, limit);
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Reciprocal Rank Fusion: score = sum(1 / (k + rank)) for each ranking list.
     */
    private List<RagResult> fuseWithRRF(List<RagEmbedding> vectorResults,
                                        List<RagEmbedding> keywordResults,
                                        int limit) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, RagEmbedding> entities = new HashMap<>();

        // Score from vector ranking
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            RagEmbedding e = vectorResults.get(rank);
            scores.merge(e.getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            entities.putIfAbsent(e.getId(), e);
        }

        // Score from keyword ranking
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            RagEmbedding e = keywordResults.get(rank);
            scores.merge(e.getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            entities.putIfAbsent(e.getId(), e);
        }

        // Sort by fused score descending and convert to RagResult
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    RagEmbedding e = entities.get(entry.getKey());
                    return new RagResult(
                            e.getContent(),
                            e.getSourceId(),
                            e.getContentType(),
                            entry.getValue(),
                            parseMetadata(e.getMetadata())
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert a natural language query to a PostgreSQL tsquery string.
     * Splits on whitespace and joins with '&' (AND).
     */
    private String toTsQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String[] words = query.trim().split("\\s+");
        List<String> sanitized = new ArrayList<>();
        for (String word : words) {
            // Remove non-alphanumeric characters for safety
            String clean = word.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "");
            if (!clean.isEmpty()) {
                sanitized.add(clean);
            }
        }
        return String.join(" & ", sanitized);
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private Map<String, String> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
