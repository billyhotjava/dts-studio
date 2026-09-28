package com.yuzhi.dts.copilot.ai.service.rag.dto;

import java.util.Map;

/**
 * A single result from RAG retrieval.
 *
 * @param content     the text content of the chunk
 * @param sourceId    identifier of the source document/entity
 * @param contentType type of content (e.g. "schema", "document", "sql")
 * @param score       relevance score (higher is better)
 * @param metadata    additional key-value metadata
 */
public record RagResult(
        String content,
        String sourceId,
        String contentType,
        double score,
        Map<String, String> metadata
) {}
