# AK-03: 用户身份传递与会话建立

**状态**: READY
**依赖**: AK-02

## 目标

实现从请求头提取用户身份信息，建立用户上下文，支持 copilot 内部的用户关联操作（审计、会话、权限等）。

## 技术设计

### 用户身份头

```
X-DTS-User-Id: user001          — 用户 ID（必需）
X-DTS-User-Name: 张三           — 用户名
X-DTS-Display-Name: 张三        — 显示名
X-DTS-Roles: ROLE_USER,ROLE_ADMIN — 角色列表
X-DTS-Dept: 运营部              — 部门（可选）
```

### 用户上下文

```java
public record CopilotUserContext(
    String userId,
    String userName,
    String displayName,
    List<String> roles,
    String dept,
    String apiKeyId  // 哪个 API Key 发起的请求
) {}

// ThreadLocal 存储，请求结束清理
public class CopilotUserContextHolder {
    static ThreadLocal<CopilotUserContext> CONTEXT = new ThreadLocal<>();
}
```

### 用户自动注册

首次出现的用户 ID 自动在 copilot 内部创建用户记录（用于关联会话、审计等）。

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/security/CopilotUserContext.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/security/CopilotUserContextHolder.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/security/UserContextFilter.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/CopilotUser.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/repository/CopilotUserRepository.java`（新建）

## 完成标准

- [ ] 请求头中的用户信息正确提取到 CopilotUserContext
- [ ] 无 X-DTS-User-Id 头时使用 API Key 名称作为默认用户
- [ ] 首次出现的用户自动注册
- [ ] 审计日志和 Chat 会话正确关联用户
