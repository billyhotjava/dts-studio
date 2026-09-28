# AK-02: API Key 认证过滤器（copilot-ai）

**状态**: READY
**依赖**: AK-01

## 目标

实现 Spring Security 过滤器，拦截所有 API 请求验证 API Key。

## 技术设计

### 认证流程

```
请求 → ApiKeyAuthFilter
    ├─ 提取 Authorization: Bearer cpk_xxx
    ├─ SHA-256 哈希 → 查询 api_key 表
    ├─ 验证状态（ACTIVE）、过期时间、IP 白名单
    ├─ 通过 → 设置 SecurityContext
    └─ 失败 → 401 Unauthorized
```

### 白名单路径

```java
// 无需认证的路径
"/actuator/health", "/actuator/info",
"/api/health", "/api/info"
```

### SecurityConfiguration

```java
@Configuration
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/api/health", "/api/info").permitAll()
                .requestMatchers("/api/auth/keys/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/security/ApiKeyAuthFilter.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/security/ApiKeyAuthentication.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/config/SecurityConfiguration.java`（新建）

## 完成标准

- [ ] 无 Key 请求返回 401
- [ ] 有效 Key 请求通过认证
- [ ] 过期 Key 返回 401
- [ ] 已吊销 Key 返回 401
- [ ] 白名单路径无需认证
