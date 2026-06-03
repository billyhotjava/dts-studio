# F3: 场景接入套件产品化

**优先级**: P1
**状态**: READY

## 目标

把"接入一个新业务场景"所需的范式（新 `CatalogDomain` 行 + `<scene>_*` dbt namespace + 场景 semantic pack + Trino catalog + glossary 派生 + 路由接线）固化为**可复制、可脚手架**的接入套件——这才是"可自由分拆、适配多场景"的真正资产（架构判断：复用范式，非抽模块）。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 套件骨架定义（六要素清单 + 模板） | P1 | READY | S31-F5,S32-F1,F2 |
| T02 | 场景脚手架脚本 | P1 | READY | T01 |
| T03 | 升级 Sprint-30 F5 checklist 为可执行套件 | P1 | READY | T01 |

## Task 明细

### T01 套件骨架定义
- **目标**：明确接入一个场景的最小六要素与各自模板。
- **设计**：六要素 = ①`CatalogDomain` 行（数据，非代码）②dbt `<scene>_*` 五层 namespace 模板 ③场景 semantic pack 模板（对象/fewShots/guardrails 生成区接 Sprint-31 sync）④Trino catalog（按 source 一个）⑤glossary/数据标准派生 ⑥路由接线（场景域纳入 F1 阶梯）。每要素给空白模板 + 报花/财务的填好样例。
- **影响**：`assets/scenario-onboarding-kit/`（模板集）。
- **验证**：六要素模板齐全，能映射到现有报花/财务两域。

### T02 场景脚手架脚本
- **目标**：一条命令生成新场景骨架，降低人工接入成本。
- **设计**：脚手架脚本（复用 webapp 已有 `scripts/scaffold-*` 风格 + dbt 生成）：输入场景 code/域 → 产出 dbt namespace 目录、pack 模板、Trino catalog 配置片段、CatalogDomain 注册指引、路由注册位。生成物是"待填的正确骨架"，非自动建模。
- **影响**：`scripts/scaffold-scenario.*` + 模板引用 T01。
- **验证**：脚手架出一个 dryrun 场景，目录/配置结构正确、可被后续填充。

### T03 升级 checklist 为可执行套件
- **目标**：把 Sprint-30 F5 的空白域 onboarding checklist 与本套件合并为一份"可执行接入手册"。
- **设计**：checklist 每步对应套件中的模板/脚本/验证命令，从"纸面清单"升级为"照单跑命令"。并入 Sprint-31 语义收口 onboarding 的口径治理步骤。
- **影响**：`assets/scenario-onboarding-kit/README.md`（总入口）。
- **验证**：F4 用本手册实际接入一个域，全程照手册可走通。

## 完成标准

- [ ] 六要素套件模板齐全，含两域填好样例
- [ ] 脚手架脚本可一键生成新场景骨架
- [ ] 可执行接入手册成文，F4 据此实战
