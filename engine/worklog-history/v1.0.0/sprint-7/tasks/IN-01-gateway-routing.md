# IN-01: 园林平台 Gateway 路由配置

**状态**: READY
**依赖**: AK-05

## 目标

在园林平台的 rs-gateway 中添加路由规则，将 `/copilot/**` 请求转发到 dts-copilot 服务。

## 技术设计

### Gateway 路由配置（Nacos YAML）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: copilot-ai
          uri: http://copilot-ai:8091
          predicates:
            - Path=/copilot/ai/**
          filters:
            - StripPrefix=2
            - name: AddRequestHeader
              args:
                name: Authorization
                value: "Bearer ${COPILOT_API_KEY}"
            - name: AddRequestHeader
              args:
                name: X-DTS-User-Id
                value: "#{headers['X-Access-User-Id']}"

        - id: copilot-analytics
          uri: http://copilot-analytics:8092
          predicates:
            - Path=/copilot/analytics/**
          filters:
            - StripPrefix=2

        - id: copilot-webapp
          uri: http://copilot-webapp:80
          predicates:
            - Path=/copilot/web/**
          filters:
            - StripPrefix=2
```

### 认证转换

Gateway 从园林平台 JWT Token 中提取用户信息，注入到 copilot 请求头中：
- `X-Access-User-Id` → `X-DTS-User-Id`
- `X-Access-User-Name` → `X-DTS-User-Name`
- `X-Access-Nick-Name` → `X-DTS-Display-Name`

同时注入 API Key 用于 copilot 认证。

## 影响文件

- `adminapi/api-config/gateway-bootstrap-dev.yml`（修改：添加 copilot 路由）
- 或 Nacos 配置中心直接添加

## 完成标准

- [ ] `/copilot/ai/api/ai/copilot/status` 正确转发到 copilot-ai
- [ ] 用户身份正确从 JWT 转换到 X-DTS-* 头
- [ ] API Key 正确注入
- [ ] 无认证信息泄露
