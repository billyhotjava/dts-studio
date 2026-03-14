package com.yuzhi.dts.copilot.ai.service.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.RagEmbedding;
import com.yuzhi.dts.copilot.ai.repository.RagEmbeddingRepository;
import com.yuzhi.dts.copilot.ai.service.rag.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Manages vector storage: embedding, inserting, searching, and deleting
 * text chunks in the rag_embedding table via pgvector.
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final EmbeddingService embeddingService;
    private final RagEmbeddingRepository ragEmbeddingRepository;

    public VectorStoreService(EmbeddingService embeddingService,
                              RagEmbeddingRepository ragEmbeddingRepository) {
        this.embeddingService = embeddingService;
        this.ragEmbeddingRepository = ragEmbeddingRepository;
    }

    /**
     * Embed the given text and store it in the vector store.
     *
     * @param contentType type of content (e.g. "schema", "document")
     * @param sourceId    source identifier
     * @param text        the text content to embed
     * @param metadata    optional metadata key-value pairs
     */
    @Transactional
    public void insert(String contentType, String sourceId, String text, Map<String, String> metadata) {
        float[] embedding = embeddingService.embed(text);
        if (embedding == null) {
            log.warn("Failed to generate embedding for content type={}, sourceId={}", contentType, sourceId);
            return;
        }

        RagEmbedding entity = new RagEmbedding();
        entity.setContentType(contentType);
        entity.setSourceId(sourceId);
        entity.setContent(text);
        entity.setEmbedding(toVectorLiteral(embedding));

        if (metadata != null && !metadata.isEmpty()) {
            try {
                entity.setMetadata(mapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize metadata: {}", e.getMessage());
            }
        }

        ragEmbeddingRepository.save(entity);
        log.debug("Inserted RAG embedding: type={}, sourceId={}", contentType, sourceId);
    }

    /**
     * Search for the most similar text chunks by cosine similarity.
     *
     * @param queryEmbedding the query embedding vector
     * @param limit          max number of results
     * @return list of matching embeddings ordered by similarity
     */
    public List<RagEmbedding> searchByVector(float[] queryEmbedding, int limit) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        return ragEmbeddingRepository.searchByVector(vectorLiteral, limit);
    }

    /**
     * Delete all embeddings for the given content type and source.
     */
    @Transactional
    public void delete(String contentType, String sourceId) {
        ragEmbeddingRepository.deleteByContentTypeAndSourceId(contentType, sourceId);
        log.debug("Deleted RAG embeddings: type={}, sourceId={}", contentType, sourceId);
    }

    /**
     * Converts a float array to a pgvector literal string, e.g. "[0.1,0.2,0.3]".
     */
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
}
