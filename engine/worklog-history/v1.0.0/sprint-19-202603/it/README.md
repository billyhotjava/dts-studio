# Sprint-19 IT

本目录用于存放 `Copilot -> 分析草稿 -> 查询 -> 可视化` 的集成测试与验收脚本。

当前计划中的验证包括：

- Copilot 保存草稿
- 从草稿打开查询编辑器
- 草稿转正式查询
- 草稿创建可视化
- 多端入口复用 smoke
- 仪表盘自动接入草稿查询卡片
- 报告工厂默认带入草稿会话来源
- 大屏 AI 生成器继承草稿业务语义

## 当前可执行资产

- `test_analysis_draft_workflow.sh`
  - 运行草稿资产中心、Copilot handoff 与多端桥接的前端契约测试
  - 运行 `typecheck`
  - 运行生产构建
- `analysis-draft-acceptance-matrix.md`
  - 记录当前已覆盖场景与待补真人联调
