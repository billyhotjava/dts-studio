# Sprint-15 IT

本目录用于保存 `Sprint-15: Copilot 语音输入支持` 的集成测试计划和验收记录。

## 覆盖范围

- Web Speech API 兼容性（桌面 + 移动端）
- 语音识别准确率（中文业务问句）
- 交互状态机（idle → listening → processing → idle）
- 错误处理（权限拒绝、网络错误、超时）
- 与现有文字输入的兼容

## 测试资产

- `VI-06-it-compatibility-test.md`（任务文档中）
- 测试矩阵：4 桌面浏览器 + 4 移动端环境
- 功能用例：10 个场景
