# BA-07: BI 引擎集成测试

**状态**: READY
**依赖**: BA-01~06

## 目标

编写 BI 引擎的集成测试套件。

## 测试场景

1. **数据源注册链路**: 注册 → 测试连通 → 获取表列表 → 获取列列表
2. **查询链路**: 创建卡片 → 执行查询 → 获取结果
3. **仪表盘链路**: 创建 → 添加卡片 → 保存 → 加载
4. **AI 屏幕生成**: 调用 AI 生成 → 预览 → 发布
5. **公开链接**: 创建公开链接 → 无认证访问

## 影响文件

- `dts-copilot-analytics/src/test/java/.../web/rest/DatabaseResourceIT.java`（新建）
- `dts-copilot-analytics/src/test/java/.../web/rest/CardResourceIT.java`（新建）
- `dts-copilot-analytics/src/test/java/.../web/rest/DashboardResourceIT.java`（新建）

## 完成标准

- [ ] 所有集成测试通过
- [ ] 无对 dts-stack 任何模块的依赖
