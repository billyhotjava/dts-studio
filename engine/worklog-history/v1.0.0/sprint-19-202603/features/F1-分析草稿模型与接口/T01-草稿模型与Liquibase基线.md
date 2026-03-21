# T01: 草稿模型与 Liquibase 基线

**优先级**: P0
**状态**: DONE
**依赖**: 无

## 目标

新增 `analysis_draft` 表和领域模型，形成分析草稿的持久化基础。

## 技术设计

- 新增 Liquibase changelog
- 新增 JPA Entity / Repository
- 字段覆盖来源、问题、SQL、解释、状态、关联资产

## 影响范围

- `dts-copilot-analytics` schema
- analytics domain / repository

## 验证

- [x] Entity 映射测试
- [x] Liquibase 基线可编译加载

## 完成标准

- [x] `analysis_draft` 表结构稳定
- [x] 草稿核心字段齐全
