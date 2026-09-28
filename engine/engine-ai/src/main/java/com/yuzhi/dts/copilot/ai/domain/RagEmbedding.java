package com.yuzhi.dts.copilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/**
 * JPA entity for the rag_embedding table.
 * Stores text chunks with their vector embeddings for RAG retrieval.
 * <p>
 * The {@code embedding} column is stored as a pgvector {@code vector(1024)} in PostgreSQL.
 * JPA maps it as a {@code String} since there is no standard JPA type for pgvector;
 * native queries handle the casting.
 */
@Entity
@Table(name = "rag_embedding")
public class RagEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @NotBlank
    @Column(name = "source_id", nullable = false, length = 256)
    private String sourceId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * pgvector embedding stored as text representation, e.g. "[0.1,0.2,...]".
     * Actual column type is vector(1024).
     */
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private String embedding;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String metadata;

    /**
     * Pre-computed tsvector column for full-text search.
     * Maintained by a PostgreSQL trigger or generated column.
     */
    @Column(name = "tsv", insertable = false, updatable = false, columnDefinition = "tsvector")
    private String tsv;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public RagEmbedding() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
