# AA-02: RAG 向量存储与混合检索抽取

**状态**: READY
**依赖**: AA-01

## 目标

从 dts-platform 抽取 VectorStoreService、HybridSearchService 和 GovernanceRagService，实现向量 + BM25 + RRF 混合检索。

## 技术设计

### 来源文件

- `dts-platform/service/ai/rag/VectorStoreService.java`
- `dts-platform/service/ai/rag/HybridSearchService.java`
- `dts-platform/service/ai/rag/GovernanceRagService.java`

### 混合检索流程

```
查询 → EmbeddingService 向量化
     ├─ VectorStoreService: pgvector cosine similarity (top-K)
     ├─ KeywordSearch: PostgreSQL ts_vector/ts_query (top-K)
     └─ RRF Fusion: score = Σ(1/(k+rank)), k=60 → 合并排序
```

### 改造

- `GovernanceRagService` 重命名为 `RagService`（去掉 Governance 前缀）
- 保留通用知识检索能力
- 去掉治理专用的种子数据

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/rag/VectorStoreService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/rag/HybridSearchService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/rag/RagService.java`（新建）

## 完成标准

- [ ] 向量检索返回语义相似的结果
- [ ] BM25 关键词检索正常工作
- [ ] RRF 融合排序正确
- [ ] RagService 可被 AiCopilotService 和 Agent 调用
