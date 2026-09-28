package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.RagEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link RagEmbedding}.
 * Uses native queries for pgvector cosine similarity and tsvector full-text search.
 */
@Repository
public interface RagEmbeddingRepository extends JpaRepository<RagEmbedding, Long> {

    /**
     * Vector similarity search using pgvector cosine distance.
     * Returns results ordered by cosine similarity (descending).
     *
     * @param queryEmbedding the query embedding as a string vector literal, e.g. "[0.1,0.2,...]"
     * @param limit          maximum number of results
     * @return list of embeddings with the highest cosine similarity
     */
    @Query(value = """
            SELECT r.*, 1 - (r.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
            FROM rag_embedding r
            ORDER BY r.embedding <=> CAST(:queryEmbedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<RagEmbedding> searchByVector(@Param("queryEmbedding") String queryEmbedding,
                                      @Param("limit") int limit);

    /**
     * Full-text keyword search using PostgreSQL tsvector/tsquery.
     *
     * @param tsQuery the tsquery string (e.g. "foo & bar")
     * @param limit   maximum number of results
     * @return list of embeddings matching the keyword query, ordered by relevance
     */
    @Query(value = """
            SELECT r.*, ts_rank(r.tsv, to_tsquery('english', :tsQuery)) AS rank
            FROM rag_embedding r
            WHERE r.tsv @@ to_tsquery('english', :tsQuery)
            ORDER BY rank DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<RagEmbedding> searchByKeyword(@Param("tsQuery") String tsQuery,
                                       @Param("limit") int limit);

    /**
     * Delete all embeddings for a given content type and source.
     */
    @Modifying
    @Query("DELETE FROM RagEmbedding r WHERE r.contentType = :contentType AND r.sourceId = :sourceId")
    void deleteByContentTypeAndSourceId(@Param("contentType") String contentType,
                                        @Param("sourceId") String sourceId);

    /**
     * Find all embeddings for a given content type and source.
     */
    List<RagEmbedding> findByContentTypeAndSourceId(String contentType, String sourceId);
}
