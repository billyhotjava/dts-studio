# Sprint-10: 园林业务语义层与双通道 NL2SQL (BG)

**前缀**: BG (Business Grounding)
**状态**: READY
**目标**: 为 `dts-copilot` 建立面向 `adminapi/adminweb` 的业务语义层和双通道 NL2SQL 架构，优先打通项目履约与现场业务两个主题域。

## 背景

代码审查已经确认：

- `adminapi/adminweb` 的主业务轴集中在 `project / flowerbiz / tasknew / pendulum`
- `dts-copilot` 已具备外部业务库接入、表字段元数据、同义词、指标对象、NL2SQL eval case 等底座
- 当前问题不在“能不能查库”，而在“能不能稳定理解业务、走对口径、受权限约束地执行”

因此本 Sprint 不再继续堆 prompt 或零散业务 Tool，而是建设一套可复用的业务 Grounding 层。

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| BG-01 | 业务域盘点与语义源映射 | READY | IN-03, NV-07 |
| BG-02 | 语义对象/字段/关系模型基线 | READY | BG-01 |
| BG-03 | 项目履约语义包 | READY | BG-02 |
| BG-04 | 现场业务语义包 | READY | BG-02 |
| BG-05 | Join Contract 与 Allowed Tables 编译 | READY | BG-03, BG-04 |
| BG-06 | 指标口径与 Metric Store 对齐 | READY | BG-03, BG-04 |
| BG-07 | 意图路由与双通道判定 | READY | BG-05, BG-06 |
| BG-08 | 直连通道上下文编译与权限桥接 | READY | BG-05, BG-07 |
| BG-09 | 轻量 ELT 主题层与增量同步 | READY | BG-03, BG-04, BG-06 |
| BG-10 | IT 集成测试与验收矩阵 | READY | BG-01~09 |

## 交付范围

### 主题域

- 项目履约：项目、客户、合同、点位、项目绿植、履约任务
- 现场业务：加花、减花、换花、转移、回收、初摆、执行任务

### 运行时能力

- 问题路由：区分主题域和查询类型
- 双通道执行：
  - 明细/现场追问 -> 业务只读库直连
  - 指标/趋势分析 -> 轻量 ELT 主题层
- 统一业务上下文编译：业务对象、允许表、允许 join、指标、同义词、问句样本

## 完成标准

- [ ] Copilot 能对项目履约和现场业务问题进行主题域识别
- [ ] 直连通道不再向模型暴露全库，而是按语义包和 join contract 收缩上下文
- [ ] 指标类问题可以优先走统一口径定义
- [ ] 轻量 ELT 主题层至少落两个 v1 对象：项目履约主题层、现场业务事件事实层
- [ ] 查询权限由 `analytics` 统一收口，不允许 Copilot 绕过权限直接执行任意 SQL
- [ ] `worklog/v1.0.0/sprint-10/it` 中具备完整的集成测试矩阵与验证入口

## IT 验证范围

- `adminweb / adminapi` 业务语义源可追溯
- `copilot-ai` 主题域路由、上下文编译、SQL 生成
- `copilot-analytics` 权限、执行、缓存、主题层查询
- 双通道链路：
  - 明细追问链路
  - 指标趋势链路

## 优先级说明

本 Sprint 是 `v1.0.0` 从“能连库”到“懂业务”的关键升级。未完成本 Sprint 前，不建议将 `adminapi/adminweb` 的 NL2SQL 能力对普通业务用户广泛开放。
