# Skills Directory

DTS AI Decision OS 技能清单。与 `.rules/60-skills/` 中的设计规范配合使用。

## 目录结构

```
.skills/
├── 00-platform/             # 平台内置技能 (shipped with dts-stack)
│   ├── data-skills           # 数据操作: schema查询/执行查询/导入导出
│   ├── ontology-skills       # 本体操作: 语义搜索/类型建议/血缘查询
│   ├── query-skills          # 智能查询: NL2SQL/结果解释/图表生成
│   ├── governance-skills     # 数据治理: 分级分类/异常检测/规则建议
│   ├── report-skills         # 报表: 生成/摘要/定时推送
│   └── action-skills         # 动作: 页面导航/创建审批/通知
├── 10-data/                 # 数据层技能
│   ├── connector-skills      # 连接器: 元数据发现/连接测试/CDC/映射建议
│   └── quality-skills        # 质量: 规则执行/漂移检测/修复建议/自动清洗
├── 20-ai/                   # AI 核心技能
│   ├── agent-skills          # Agent: 意图路由/步骤规划/决策解释/反馈收集
│   └── eval-skills           # 评测: 执行套件/版本对比/用例生成/Bad Case分析
├── 30-industry/             # 行业技能 (AppPack 提供)
│   ├── energy-skills         # 电力: 负荷预测/缺陷检测/工单/风险评估
│   ├── research-skills       # 科研: 里程碑跟踪/质量分析/NCR评审/批次对比
│   └── manufacturing-skills  # 制造: OEE/预测维护/排程优化/质量漂移
└── 40-devops/               # 运维技能
    └── ops-skills            # 运维: 健康检查/故障诊断/扩缩容/备份/Pack管理
```

## 状态说明

当前所有技能文件为 **PLACEHOLDER**（占位），定义了技能名称、类型、描述和优先级。
实现阶段将按照 `.rules/60-skills/skill-design.rules` 中的规范逐一开发。

## 技能统计

| 类别 | 数量 | 状态 |
|------|------|------|
| Platform (00) | 17 | Placeholder |
| Data (10) | 6 | Placeholder |
| AI (20) | 9 | Placeholder |
| Industry (30) | 18 | Placeholder |
| DevOps (40) | 7 | Placeholder |
| **Total** | **57** | — |
