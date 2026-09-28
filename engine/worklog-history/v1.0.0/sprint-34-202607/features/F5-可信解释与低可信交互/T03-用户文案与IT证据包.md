# T03: 用户文案与 IT 证据包

**优先级**: P2
**状态**: DONE
**依赖**: T02

## 目标

沉淀业务可读文案和 IT 证据，证明 Sprint-34 的准确性能力可演示、可复测。

## 技术设计

- 文案避免 SQL 术语优先，使用“数据来源、口径、对账、更新时间”。
- IT 证据覆盖同步 chat、流式 chat、黄金集、scorecard、低可信降级。
- 每个证据带命令和期望结果。

## 影响范围

- `it/README.md`。
- demo notes。
- 用户帮助文案。

## 落地结果

- IT 证据包补充 `MessageList.platformIndicator.test.tsx`，覆盖 UNTRUSTED/L0 profile 的低可信提示。
- 用户侧文案固定为“可信度不足”“不作为统计结论”“请先执行入湖、dbt 构建或补齐对账证据”。
- 复测命令写入 `it/README.md` 和 `test_sprint34_accuracy_plan.sh`。

## 验证

- [x] IT README 没有空占位。
- [x] 每条证据可重跑。
- [x] 文案对非技术用户可读。

## 完成标准

- [x] Sprint-34 能用证据包验收，而不是只看开发自述。
