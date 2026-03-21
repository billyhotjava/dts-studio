# Sprint-20 IT

本目录用于存放“菜单页 + Copilot 平级协同”的自动化 smoke、验收矩阵和真人联调清单。

## 自动化 smoke

- 脚本：`it/test_analysis_workspace_peer_entry.sh`
- 覆盖：
  - 统一来源上下文组件
  - 查询资产中心增强
  - Copilot 与查询往返
  - 多端来源追溯
  - 前端 `typecheck`
  - 前端 `build`

运行方式：

```bash
bash worklog/v1.0.0/sprint-20-202603/it/test_analysis_workspace_peer_entry.sh
```

## 验收矩阵

- 文档：`it/acceptance-matrix.md`
- 用途：对齐双入口协同的关键路径、预期和自动化证据

## 真人联调清单

### A. 查询资产中心

1. 打开 `/questions`
2. 确认可见：
   - `全部`
   - `正式查询`
   - `Copilot 草稿`
   - `最近分析`
3. 切换来源筛选、状态筛选、排序
4. 确认草稿条目显示：
   - 来源
   - 状态
   - 原始问题摘要

### B. Copilot 到查询草稿

1. 在任意有数据源的页面打开 Copilot
2. 询问一个可生成 SQL 的问题
3. 在回答动作区依次验证：
   - `执行查询`
   - `保存草稿`
   - `在查询中打开`
   - `创建可视化`
4. 从 `在查询中打开` 进入后，确认查询编辑页顶部显示：
   - 来源：`AI Copilot`
   - 原始问题
   - 数据源
   - 草稿状态

### C. 草稿晋升与多端追溯

1. 从查询编辑页保存为正式查询
2. 分别进入：
   - 仪表盘
   - 报告工厂
   - 大屏
3. 确认页面内仍显示来源上下文面板
4. 确认可回到：
   - 查询草稿
   - 来源查询
   - 固定报表（若存在）

### D. 采购域业务回归

基线见 [procurement-query-regression.md](/opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-20-202603/it/procurement-query-regression.md)。

重点验证：

1. 问句：`查询2026年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计`
2. 环境：`ai.yuzhicloud.com`
3. 数据源：`rs_cloud_flower`
4. 口径：精确匹配 `good_name='绿萝'`
5. 预期：
   - 不再错误命中 `title like '%绿萝%'`
   - 不再错误走 `i_pendulum_purchase*`
   - 聚合结果可对齐：
     - `佟玉华 = 5170.50`
     - `聂良辉 = 1190.00`

## 输出要求

真人联调结束后，至少记录：

- 日期
- 环境
- 使用账号
- 是否通过 A/B/C/D 四类检查
- 若失败，对应页面路径和截图/日志
