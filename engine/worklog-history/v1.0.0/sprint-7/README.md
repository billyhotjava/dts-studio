# Sprint-7: 园林平台集成与端到端验证 (IN)

**前缀**: IN (Integration)
**状态**: READY
**目标**: 将 dts-copilot 集成到馨懿诚园林管理平台，验证端到端的 AI 助手 + BI 分析能力。

## 背景

园林平台技术栈：
- 后端：Spring Cloud + Nacos + JWT/Redis（adminapi）
- PC 前端：Vue 2 + Element UI（adminweb）
- 移动端：uni-app（app）
- 数据库：MySQL

集成方式：
- 园林平台 Gateway（rs-gateway）添加路由，转发 `/copilot/` 到 dts-copilot 服务
- adminweb 通过 iframe 嵌入 copilot-webapp
- 后端通过 API Key 调用 copilot-ai 的 AI 能力
- copilot-analytics 注册园林平台的 MySQL 作为数据源

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| IN-01 | 园林平台 Gateway 路由配置 | READY | AK-05 |
| IN-02 | adminweb 嵌入 copilot-webapp | READY | FE-04 |
| IN-03 | 园林平台数据源注册 | READY | BA-02 |
| IN-04 | 园林业务 Tool 扩展示例 | READY | AA-04, IN-03 |
| IN-05 | 端到端集成测试 | READY | IN-01~04 |

## 完成标准

- [ ] 园林平台用户通过 adminweb 菜单可直接访问 BI 仪表盘
- [ ] AI Copilot 可对园林平台 MySQL 数据进行自然语言查询
- [ ] 园林业务 Tool 可查询项目、花卉、财务等数据
- [ ] 端到端链路：登录园林平台 → 打开 Copilot → 提问 → AI 生成 SQL → 返回结果

## IT 验证命令

```bash
# 验证 Gateway 路由
curl -H "Authorization: Bearer {jwt}" http://localhost:7091/copilot/api/ai/copilot/status

# 验证数据源
curl -X POST http://localhost:8091/api/datasources \
  -H "Authorization: Bearer cpk_xxx" \
  -d '{"name": "garden-mysql", "dbType": "mysql", "jdbcUrl": "jdbc:mysql://localhost:3306/flowers"}'

# 验证 NL2SQL
curl -X POST http://localhost:8091/api/ai/copilot/nl2sql \
  -H "Authorization: Bearer cpk_xxx" \
  -d '{"naturalLanguage": "查询所有在租项目的花卉总数", "dataSourceId": 1}'
```

## 优先级说明

IN-01/IN-02/IN-03 可并行 → IN-04 依赖 IN-03 → IN-05 收尾
