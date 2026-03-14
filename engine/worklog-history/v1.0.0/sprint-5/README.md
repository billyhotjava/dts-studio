# Sprint-5: BI 分析引擎抽取 (BA)

**前缀**: BA (BI Analytics)
**状态**: READY
**目标**: 从 dts-analytics 完整 fork BI 分析引擎，重构包名，替换对外部服务（dts-platform/dts-ingestion）的依赖为对 copilot-ai 的调用。

## 背景

dts-analytics 是一个相对独立的 BI 服务，Maven 无直接依赖其他 dts-stack 模块，通过 REST 客户端与 dts-platform/dts-ingestion 通信。抽取时需要：

1. fork 完整代码，包名从 `com.yuzhi.dts.analytics` 改为 `com.yuzhi.dts.copilot.analytics`
2. `PlatformInfraClient`（数据源管理）→ 改为调用 copilot-ai 的数据源 API
3. `PlatformAiNativeClient`（AI 屏幕生成）→ 改为调用 copilot-ai 的 AI API
4. `AiConfigClient`（AI 配置）→ 改为调用 copilot-ai 的配置 API
5. `PlatformTrustedUserService`（认证）→ 替换为 API Key 认证（sprint-4 已完成）

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| BA-01 | dts-analytics 核心代码 fork 与包名重构 | READY | SC-03 |
| BA-02 | 数据源管理独立化（从 copilot-ai 获取） | READY | BA-01, AE-06 |
| BA-03 | 认证层替换（PlatformTrustedUser → ApiKeyAuth） | READY | BA-01, AK-04 |
| BA-04 | AI 屏幕生成对接 copilot-ai | READY | BA-01, AE-06 |
| BA-05 | SQL Workbench + 仪表盘 + 报表功能验证 | READY | BA-01~04 |
| BA-06 | 公开链接与嵌入式分析 | READY | BA-05 |
| BA-07 | BI 引擎集成测试 | READY | BA-01~06 |

## 完成标准

- [ ] copilot-analytics 编译通过，无对 dts-stack 的依赖
- [ ] 数据源注册和查询正常工作
- [ ] SQL Workbench 可执行查询
- [ ] 仪表盘创建、编辑、查看正常
- [ ] AI 屏幕生成调用 copilot-ai 成功
- [ ] 公开链接和嵌入式分析可用

## IT 验证命令

```bash
cd dts-copilot/dts-copilot-analytics && mvn compile

# 注册数据源
curl -X POST http://localhost:8092/api/databases \
  -H "Authorization: Bearer cpk_xxx" \
  -H "Content-Type: application/json" \
  -d '{"name": "garden-db", "engine": "mysql", "details": {"host": "...", "port": 3306, "db": "flowers"}}'

# 执行查询
curl -X POST http://localhost:8092/api/cards \
  -H "Authorization: Bearer cpk_xxx" \
  -H "Content-Type: application/json" \
  -d '{"dataset_query": {"type": "native", "native": {"query": "SELECT COUNT(*) FROM p_project"}}}'
```

## 优先级说明

BA-01 最先 → BA-02/BA-03/BA-04 可并行 → BA-05 汇聚验证 → BA-06 → BA-07 收尾
