# AA-01: pgvector schema + Embedding 服务迁移

**状态**: READY
**依赖**: AE-01

## 目标

将 dts-platform 的 pgvector RAG 存储表和 Embedding 服务迁移到 copilot-ai，在 `copilot_ai` schema 下创建向量存储。

## 技术设计

### Liquibase 迁移（copilot_ai schema）

```sql
CREATE TABLE copilot_ai.rag_embedding (
    id              BIGSERIAL PRIMARY KEY,
    content_type    VARCHAR(32) NOT NULL,
    source_id       VARCHAR(255) NOT NULL,
    content_text    TEXT NOT NULL,
    embedding       vector(1024),
    metadata        JSONB DEFAULT '{}',
    tenant_id       VARCHAR(64),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_rag_embedding_hnsw
    ON copilot_ai.rag_embedding USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

ALTER TABLE copilot_ai.rag_embedding ADD COLUMN content_tsv tsvector
    GENERATED ALWAYS AS (to_tsvector('simple', content_text)) STORED;
CREATE INDEX idx_rag_embedding_tsv ON copilot_ai.rag_embedding USING gin (content_tsv);
```

### Embedding 服务

从 dts-platform 抽取 `EmbeddingService` 接口和 `BgeM3EmbeddingService` 实现，支持 OpenAI 兼容的 `/v1/embeddings` 端点。

Ollama 内置 embedding 支持：`ollama pull bge-m3` 或通过 Ollama 的 `/v1/embeddings` 端点调用。

## 影响文件

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_002__rag_pgvector.xml`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/rag/embedding/EmbeddingService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/rag/embedding/BgeM3EmbeddingService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/RagEmbeddingRecord.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/RagEmbeddingRepository.java`（新建）

## 完成标准

- [ ] pgvector 扩展启用，`rag_embedding` 表在 `copilot_ai` schema 下创建
- [ ] HNSW 索引创建成功
- [ ] Embedding 服务可调用 Ollama 生成 1024 维向量
- [ ] 插入和余弦相似度查询验证通过
