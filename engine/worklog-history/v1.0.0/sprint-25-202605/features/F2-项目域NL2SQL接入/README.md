# F2: 项目域 NL2SQL 接入

**优先级**: P1  
**状态**: BLOCKED

## 目标

把项目域 ADS/DWS 接入 dts-copilot 智能层，使“项目实摆总览、合同到期、摆位状态分布”等问句走可信数据面。

## 阻塞

依赖 F1 产出的 ADS/DWS。当前不创建 project 语义包和 routing/template，避免让运行时路由到尚不存在的 mart。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | `project.json` 语义包与 constraints | P1 | READY | F1 |
| T02 | routing/templates/enum liquibase | P1 | READY | T01 |
| T03 | Java 目录接入与加载测试 | P1 | READY | T01, T02 |

## 完成标准

- [ ] 语义包运行时加载有单测，不只检查文件存在。
- [ ] 项目域高频问句优先命中 ADS/DWS，不扫 ODS。
- [ ] 业务对象问答带 pagePath、sourceRefs、qualityLevel、dataSurface。
- [ ] query templates 至少覆盖 8 条项目域高频问句。
