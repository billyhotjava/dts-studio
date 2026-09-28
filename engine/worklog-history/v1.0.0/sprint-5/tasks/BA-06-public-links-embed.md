# BA-06: 公开链接与嵌入式分析

**状态**: READY
**依赖**: BA-05

## 目标

验证并确保公开链接（Public Link）和嵌入式分析（Embed Token）功能在 dts-copilot 中正常工作。

## 技术设计

### 公开链接

允许将仪表盘/卡片/屏幕通过 UUID 公开访问（无需认证）。

```
GET /api/public/card/{uuid}       — 公开卡片
GET /api/public/dashboard/{uuid}  — 公开仪表盘
GET /api/public/screen/{uuid}     — 公开屏幕
```

### 嵌入式分析

业务系统前端通过 iframe 嵌入 copilot-analytics 页面，使用 embed token 认证。

### 跨域配置

```yaml
dts:
  copilot:
    analytics:
      cors:
        allowed-origins: "*"  # 生产环境应限制为具体域名
        allowed-methods: GET,POST
```

## 影响文件

- `dts-copilot-analytics/src/main/java/.../config/CorsConfiguration.java`（修改或新建）
- `dts-copilot-analytics/src/main/java/.../web/rest/PublicResource.java`（验证）

## 完成标准

- [ ] 公开链接可无认证访问
- [ ] iframe 嵌入无跨域问题
- [ ] Embed token 正确签发和验证
