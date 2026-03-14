# CS-03: Analytics 聚合 API Key 管理接口

**状态**: READY
**依赖**: AK-01, CS-02

## 目标

在 `analytics` 中代理 `dts-copilot-ai` 的 Copilot API Key 管理接口，供 webapp 统一调用。

## 技术设计

- 新增 `/api/admin/copilot/api-keys`
- 支持列表、创建、轮换、吊销
- 创建和轮换结果中只返回一次 `rawKey`

## 完成标准

- [ ] 页面可查看 key 前缀和元数据
- [ ] 创建/轮换后返回原始 key
- [ ] 原始 key 不在后续列表接口中返回

