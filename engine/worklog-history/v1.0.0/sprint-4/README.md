# Sprint-4: API Key 认证与安全体系 (AK)

**前缀**: AK (API Key)
**状态**: READY
**目标**: 实现 API Key 认证体系，替换 dts-analytics 原有的 Keycloak/Platform 信任头认证，使 dts-copilot 可通过 API Key 被任意业务系统集成。

## 背景

dts-copilot 定位为独立增值服务，需要一种通用的认证方式：
- 业务系统后端持 API Key 调用 copilot 服务
- 用户身份通过请求头传递（X-DTS-User-Id / X-DTS-User-Name / X-DTS-Roles）
- API Key 管理：生成、吊销、配额、IP 白名单

原 dts-analytics 使用 `PlatformTrustedUserService` 通过信任头认证，需替换为 API Key 认证。

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| AK-01 | API Key 数据模型与管理服务 | READY | SC-02 |
| AK-02 | API Key 认证过滤器（copilot-ai） | READY | AK-01 |
| AK-03 | 用户身份传递与会话建立 | READY | AK-02 |
| AK-04 | API Key 认证集成到 copilot-analytics | READY | AK-02, SC-03 |
| AK-05 | 认证体系集成测试 | READY | AK-01~04 |

## 完成标准

- [ ] API Key CRUD 正常工作（生成、查看、吊销）
- [ ] 无 API Key 的请求被 401 拒绝
- [ ] 有效 API Key + 用户头的请求正确建立用户上下文
- [ ] copilot-ai 和 copilot-analytics 共享同一套 API Key 认证
- [ ] 过期和已吊销的 Key 被拒绝

## IT 验证命令

```bash
# 生成 API Key（初始管理员密钥通过环境变量配置）
curl -X POST http://localhost:8091/api/auth/keys \
  -H "X-Admin-Secret: ${COPILOT_ADMIN_SECRET}" \
  -H "Content-Type: application/json" \
  -d '{"name": "garden-platform", "expiresInDays": 365}'

# 使用 API Key 调用
curl http://localhost:8091/api/ai/copilot/status \
  -H "Authorization: Bearer cpk_xxxx" \
  -H "X-DTS-User-Id: user001" \
  -H "X-DTS-User-Name: 张三"

# 无 Key 调用应被拒绝
curl -v http://localhost:8091/api/ai/copilot/status  # 预期 401
```

## 优先级说明

AK-01 → AK-02 → AK-03 顺序执行 → AK-04 可与 AK-03 并行 → AK-05 收尾
