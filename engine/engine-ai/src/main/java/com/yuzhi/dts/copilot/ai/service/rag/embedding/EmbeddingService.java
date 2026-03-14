package com.yuzhi.dts.copilot.ai.service.rag.embedding;

import java.util.List;

/**
 * Abstraction for text embedding generation.
 */
public interface EmbeddingService {

    /**
     * Generate an embedding vector for a single text.
     *
     * @param text the input text
     * @return the embedding vector, or {@code null} if the service is unavailable
     */
    float[] embed(String text);

    /**
     * Generate embedding vectors for a batch of texts.
     * Large batches are automatically split into smaller chunks.
     *
     * @param texts the input texts
     * @return list of embedding vectors in the same order, or {@code null} if unavailable
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Check whether the embedding service is reachable and operational.
     */
    boolean isAvailable();
}
