# Rules Directory

DTS AI Decision OS 工作守则。所有设计和实现决策必须遵守这些规则。

## 目录结构

```
.rules/
├── 00-foundation/           # 产品根基 — 任何人任何时候都必须遵守
│   ├── five-iron-laws        # 五条不可动摇的原则
│   └── product-philosophy    # 产品理念与定位
├── 10-architecture/         # 架构决策 — 技术选型和服务边界
│   ├── architecture-principles  # 三语言/通信/存储/部署/认证/可观测性
│   ├── service-boundaries       # 25 个组件的服务边界
│   └── apppack-protocol         # AppPack 能力包协议
├── 20-development/          # 开发规范 — 编码/提交/API/依赖
│   ├── coding-standards      # 三语言+前端编码规范
│   ├── git-workflow           # 分支/提交/PR 规范
│   ├── api-design             # gRPC/REST/Kafka 设计规范
│   └── dependency-policy      # 依赖管理/禁用清单/License
├── 30-testing/              # 测试规范 — 金字塔/门禁
│   ├── testing-strategy       # 单元/集成/E2E/AI评测策略
│   └── quality-gates          # PR/Nightly/Release/Hotfix 门禁
├── 40-deployment/           # 部署规范 — K8s/发布/运维
│   ├── deployment-ops         # K8s标准/镜像/离线/升级/监控
│   └── release-process        # 版本号/Changelog/发布检查清单
├── 50-appstack/             # AppPack 开发规范
│   ├── pack-development       # Pack 开发者指南/项目结构
│   └── industry/              # 行业规则
│       ├── _template           # 新行业模板
│       ├── energy              # 电力/能源
│       ├── research            # 科研院所
│       └── manufacturing       # 制造业
├── 60-skills/               # AI 技能规范
│   ├── skill-design           # 技能设计规范 (接口/五铁律映射/约束)
│   ├── skill-categories       # 技能分类体系
│   └── skill-lifecycle        # 技能生命周期 (Draft→Active→Retired)
└── 90-process/              # 流程规范
    └── worklog-organization   # worklog 目录组织规范
```

## 优先级

| 层级 | 优先级 | 说明 |
|------|--------|------|
| 00-foundation | HIGHEST | 五条铁律，不可违反 |
| 10-architecture | HIGH | 架构决策，变更需评审 |
| 20~60 | NORMAL | 开发/测试/部署/技能规范 |
| 90-process | NORMAL | 流程管理 |
