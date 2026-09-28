# IN-05: 端到端集成测试

**状态**: READY
**依赖**: IN-01~04

## 目标

验证从园林平台到 dts-copilot 的完整端到端链路。

## 测试场景

### 链路 1: AI 自然语言查询
```
园林平台登录 → adminweb 菜单 "AI 助手"
→ iframe 加载 copilot-webapp
→ 用户输入 "查询本月新增项目数量"
→ copilot-ai NL2SQL 生成 SQL
→ copilot-analytics 执行查询
→ 返回结果并展示
```

### 链路 2: BI 仪表盘
```
adminweb 菜单 "数据分析"
→ iframe 加载仪表盘列表
→ 创建新仪表盘 → 添加查询卡片
→ 选择园林 MySQL 数据源
→ 编写/AI 生成查询 → 可视化
→ 保存仪表盘
```

### 链路 3: Agent Tool 调用
```
AI 助手聊天 → 输入 "帮我看看花卉租赁到期即将到期的项目"
→ Agent ReAct 推理
→ 调用 GardenProjectQueryTool
→ 返回结构化结果
→ 生成自然语言摘要
```

## 影响文件

- `dts-copilot/worklog/v1.0.0/sprint-7/it/test_e2e_garden_integration.py`（新建）

## 完成标准

- [ ] 3 条端到端链路全部验证通过
- [ ] 无认证错误
- [ ] 无跨域错误
- [ ] 响应时间 < 10s（NL2SQL 含 LLM 推理）
- [ ] 集成测试脚本可重复执行
