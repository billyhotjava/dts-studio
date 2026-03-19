# BG-10: IT 集成测试与验收矩阵

**状态**: READY
**依赖**: BG-01~09

## 目标

建立本 Sprint 的 `it/` 集成测试目录和验收矩阵，覆盖 `adminweb -> copilot -> analytics -> 业务库` 的双通道链路。

## 技术设计

- 明细追问链路：
  - 问题路由到直连通道
  - 受 allowed tables / join contract / permission bridge 约束
- 指标趋势链路：
  - 问题路由到主题层通道
  - 使用统一指标口径和 ELT 主题层
- 验收维度：
  - 主题域识别
  - 通道判定
  - SQL 正确性
  - 权限约束
  - 返回结果可解释性

## IT 目录

- `worklog/v1.0.0/sprint-10/it/README.md`
- `worklog/v1.0.0/sprint-10/it/BG-10-dual-channel-nl2sql-matrix.md`

## 完成标准

- [ ] `it/` 目录存在且可作为本 Sprint 的验收入口
- [ ] 覆盖项目履约和现场业务两个主题域
- [ ] 覆盖直连通道和主题层通道
- [ ] 覆盖正向和越权/误路由负向用例
