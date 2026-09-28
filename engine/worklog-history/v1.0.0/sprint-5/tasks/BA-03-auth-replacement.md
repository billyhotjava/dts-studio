# BA-03: 认证层替换（PlatformTrustedUser → ApiKeyAuth）

**状态**: READY
**依赖**: BA-01, AK-04

## 目标

将 copilot-analytics 的认证从 PlatformTrustedUserService 完全替换为 API Key 认证。

## 技术设计

此 task 是 AK-04 的具体实现落地，确保 analytics 侧所有认证相关代码清理干净。

### 需要删除

- `PlatformTrustedUserService.java`
- `PlatformAuthProperties.java`
- 所有 `X-DTS-*` 头的信任模式解析（改为从 API Key 验证结果获取用户上下文）
- OIDC 相关配置和代码

### 需要新增/修改

- SecurityConfiguration：使用 API Key 过滤器
- AnalyticsSessionService：会话建立从 API Key 验证后触发
- AnalyticsUserRepository：用户自动注册逻辑保留但触发来源改变

## 影响文件

- 删除: `PlatformTrustedUserService.java`, `PlatformAuthProperties.java`, `OidcAuthResource.java`
- 修改: `SecurityConfiguration.java`, `AnalyticsSessionService.java`
- 新增: `ApiKeyAuthService.java`（调用 copilot-ai 验证）

## 完成标准

- [ ] 无 PlatformTrustedUserService 残留代码
- [ ] 无 OIDC 残留代码
- [ ] API Key 认证正常工作
- [ ] 用户会话正确建立
