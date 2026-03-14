# Sprint-9: Copilot 系统配置中心 (CS)

**前缀**: CS (Copilot Settings)
**状态**: IN_PROGRESS
**目标**: 为 `dts-copilot` 增加一个管理员可用的系统配置界面，集中管理站点设置、LLM Provider 配置和 Copilot API Key。

## 背景

当前 `dts-copilot-ai` 已具备 Provider 配置和 API Key 管理后端能力，`dts-copilot-analytics` 已具备 session-cookie 登录和 superuser 管理能力，但缺少一个真正可操作的系统配置入口。

本 Sprint 的目标是把这三层打通：

- `webapp` 提供管理员配置页面
- `analytics` 承担会话鉴权和服务端代理
- `ai` 持久化 LLM Provider 与 Copilot API Key

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| CS-01 | AI Provider 安全 DTO 与更新语义 | DONE | AE-03 |
| CS-02 | Analytics 聚合配置接口（站点设置 + Provider 代理） | DONE | CS-01, BA-03 |
| CS-03 | Analytics 聚合 API Key 管理接口 | DONE | AK-01, CS-02 |
| CS-04 | Webapp 配置 API 客户端 | DONE | CS-02, CS-03, FE-03 |
| CS-05 | Webapp 系统配置页面与导航入口 | DONE | CS-04 |
| CS-06 | 联调验证与回归测试 | IN_PROGRESS | CS-01~05 |
| CS-07 | Provider 模板目录增强（国际/国内主流 + 推荐模板元数据） | DONE | AE-03, CS-01 |
| CS-08 | Webapp Provider Type 下拉与推荐模板联动 | DONE | CS-04, CS-05, CS-07 |
| CS-09 | Provider 模板化交互回归验证 | DONE | CS-07, CS-08 |

## 完成标准

- [ ] 管理员可以访问 `/admin/settings/copilot`
- [ ] 站点名称可查看和修改
- [ ] LLM Provider 可新增、编辑、启停、设默认、删除、测试连接
- [ ] Provider API Key 支持手工录入，但列表和详情不回显明文
- [ ] Copilot API Key 支持创建、轮换、吊销，原始 key 仅展示一次
- [ ] 非 superuser 用户无法访问管理接口和页面
- [ ] Provider Type 使用分组下拉框，包含国际主流、中国主流、本地部署和 Custom
- [ ] 新建 Provider 默认使用推荐的标准预定义模板
- [ ] 切换 Provider Type 时可一键套用标准模板，但编辑场景保留 API Key 的“留空不修改”语义

## 依赖关系

```
CS-01 (AI Provider 安全化) ──→ CS-02 (配置聚合接口) ──→ CS-03 (API Key 聚合接口)
                                                          └─→ CS-04 (前端 API 客户端)
CS-04 ──→ CS-05 (配置页面)
CS-01~05 ──→ CS-06 (联调与回归)
```

## 优先级说明

本 Sprint 是 `v1.0.0` 面向实际运维落地的关键补齐项。完成后，`dts-copilot` 将具备独立配置 LLM 与管理员密钥的基本可运营能力，并提供可直接使用的主流 Provider 标准模板。
