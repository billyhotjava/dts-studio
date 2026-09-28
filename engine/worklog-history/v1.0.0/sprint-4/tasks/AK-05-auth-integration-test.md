# AK-05: 认证体系集成测试

**状态**: READY
**依赖**: AK-01~04

## 目标

编写认证体系的集成测试，验证 API Key 全生命周期和跨服务认证。

## 测试场景

1. **Key 管理**: 生成 → 使用 → 吊销 → 验证失败
2. **认证拦截**: 无 Key 401 / 有效 Key 200 / 过期 Key 401
3. **用户身份**: 请求头传递用户 → 上下文正确建立 → 审计关联
4. **跨服务**: API Key 同时可访问 copilot-ai 和 copilot-analytics
5. **IP 白名单**: 白名单外 IP 拒绝（如果配置）
6. **配额限制**: 超出频率限制后返回 429

## 影响文件

- `dts-copilot-ai/src/test/java/.../security/ApiKeyAuthFilterTest.java`（新建）
- `dts-copilot-ai/src/test/java/.../web/rest/ApiKeyResourceIT.java`（新建）

## 完成标准

- [ ] Key 全生命周期测试通过
- [ ] 跨服务认证测试通过
- [ ] 边界条件（过期、吊销、无效格式）全部覆盖
