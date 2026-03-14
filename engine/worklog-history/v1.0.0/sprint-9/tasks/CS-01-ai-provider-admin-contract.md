# CS-01: AI Provider 安全 DTO 与更新语义

**状态**: IN_PROGRESS
**依赖**: AE-03

## 目标

让 AI Provider 管理接口支持“前端手工输入 API Key，但后续不返回明文”，并保证编辑时空 key 不覆盖旧值。

## 技术设计

- 为 Provider 列表/详情改用 DTO 输出
- DTO 暴露 `hasApiKey` 和 `apiKeyMasked`
- 编辑请求里 `apiKey` 允许为空
- 当 `apiKey` 为空字符串时，保留数据库原值

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResource.java`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/config/AiConfigService.java`
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/dto/*`
- `dts-copilot-ai/src/test/java/...`

## 完成标准

- [ ] Provider 列表不返回明文 API Key
- [ ] 编辑时留空 API Key 不覆盖原值
- [ ] 测试连接使用服务端已保存密钥

