# Sprint-8: NL2SQL 聊天到可视化闭环 (NV)

**前缀**: NV (NL2SQL-Visualization)
**状态**: READY
**目标**: 打通 CopilotChat → NL2SQL → 自动执行 → 图表可视化的端到端链路，支持直连业务数据库。

## 背景

dts-copilot 已有完整的 NL2SQL 管线零件（schema 召回、SQL 生成、安全校验、查询执行、图表渲染、AI 聊天），但未组装成可用的端到端流程。本 Sprint 将它们串联起来。

交互模式：CopilotChat 中提问 → Agent 生成 SQL → 一键跳转 CardEditorPage 自动执行并渲染可视化。

数据源接入：预配置默认业务库 + 界面手动添加，双模式并存。

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| NV-01 | 默认数据源自动注册 | READY | BA-02 |
| NV-02 | Agent Chat 透传 datasourceId | READY | AA-06 |
| NV-03 | NL2SQL Agent 系统提示词优化 | READY | NV-02 |
| NV-04 | CopilotChat 数据源选择器 | READY | NV-01, FE-02 |
| NV-05 | CopilotChat "创建可视化" 按钮 | READY | NV-03 |
| NV-06 | CardEditorPage autorun 支持 | READY | FE-03 |
| NV-07 | 同义词字典可配置化 | READY | NV-03 |
| NV-08 | 端到端冒烟测试与评测用例 | READY | NV-01~06 |

## 完成标准

- [ ] 启动时自动注册预配置的业务数据库，元数据同步完成
- [ ] CopilotChat 支持选择数据源，提问后 Agent 返回 SQL
- [ ] 点击"创建可视化"跳转到 CardEditorPage，SQL 自动执行并渲染图表
- [ ] 评测集 pass rate ≥ 60%（单表 + 简单 JOIN 场景）

## 依赖关系

```
NV-01 (数据源注册) ──┐
NV-02 (透传 datasourceId) ──┤
NV-03 (提示词优化) ─────────┤─→ NV-04 (数据源选择器)
                             ├─→ NV-05 (可视化按钮)
NV-06 (autorun) ────────────┘
                             ↓
NV-07 (同义词) ──→ NV-08 (端到端测试)
```

NV-01/NV-02/NV-03/NV-06 可并行 → NV-04/NV-05 依赖后端 → NV-07/NV-08 收尾

## 优先级说明

Sprint-8 依赖 Sprint-2(AE)、Sprint-3(AA)、Sprint-5(BA)、Sprint-6(FE) 的核心能力。是 v1.0.0 的最后一个功能 Sprint，实现后即具备"自然语言查数据"的核心产品能力。
