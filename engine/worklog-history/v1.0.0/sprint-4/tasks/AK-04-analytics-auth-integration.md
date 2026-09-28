# AK-04: API Key 认证集成到 copilot-analytics

**状态**: READY
**依赖**: AK-02, SC-03

## 目标

将 API Key 认证集成到 copilot-analytics 服务，替换原 dts-analytics 的 PlatformTrustedUserService。

## 技术设计

### 替换方案

原 dts-analytics 的认证链：
```
请求 → PlatformTrustedUserService → 信任头验证 / Bearer → dts-platform forward-auth
```

替换为：
```
请求 → ApiKeyAuthFilter → copilot-ai /api/auth/verify（内部调用）→ 用户上下文
```

### copilot-analytics 认证流程

1. 提取 `Authorization: Bearer cpk_xxx` 头
2. 调用 copilot-ai 的内部验证端点 `POST /internal/auth/verify`
3. 验证通过后建立 analytics 内部会话
4. 后续请求使用 analytics session cookie

### copilot-ai 新增内部端点

```
POST /internal/auth/verify
  Request: { "apiKey": "cpk_xxx", "userId": "user001" }
  Response: { "valid": true, "user": { ... }, "permissions": [...] }
```

## 影响文件

- `dts-copilot-analytics/src/main/java/.../security/ApiKeyAuthService.java`（新建，替换 PlatformTrustedUserService）
- `dts-copilot-analytics/src/main/java/.../config/SecurityConfiguration.java`（修改）
- `dts-copilot-ai/src/main/java/.../web/rest/InternalAuthResource.java`（新建）

## 完成标准

- [ ] copilot-analytics 使用 API Key 认证替代 Platform 信任头
- [ ] PlatformTrustedUserService 完全移除
- [ ] copilot-analytics 可通过 API Key 访问所有端点
- [ ] 内部 auth verify 端点仅对内网可访问
