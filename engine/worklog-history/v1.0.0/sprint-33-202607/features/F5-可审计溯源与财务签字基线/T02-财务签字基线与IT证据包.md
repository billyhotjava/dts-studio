# T02: 财务签字基线 + IT 证据包

**优先级**: P1
**状态**: IN_PROGRESS
**依赖**: F1-F4

## 目标

组织财务对一次完整对账基线签字确认,并归档全部可重跑证据——把"工程证明"转化为"业务采信"。

## 技术设计

- **签字基线**:选一个完整账期,跑全套（明细对账 F1 + 汇总/凭证 tie-out F2 + 不变量 F3 + 差分网格 F4）,产出基线对账报告,财务复核签字。签字版作为后续漂移的对照基准。
- **证据包** `it/`:
  - 明细对账日志（F1-T02/T03）
  - 汇总双路 + 凭证 tie-out（F2）
  - 不变量回归 + 静态拦截（F3,绿）
  - 差分网格 + 记分卡（F4）
  - 可审计溯源样例（F5-T01）
  - 每条带可重跑命令,记分卡脚本作为 Sprint-33 DONE 门禁。

## 影响范围

- 产出 `it/README.md` + `it/evidence/` + `assets/finance-signoff-baseline.md`

## 当前进展

- 已落地 `FinanceSignoffBaselineRegistry`：机器化读取 `finance-signoff-baseline.v1.json`，固化账期、scorecard policy、必需证据、重跑命令、签字角色、资产路径和 IT 脚本。
- 已落地 `FinanceSignoffBaselineService`：组装签字基线报告，校验 F1-F5 证据是否齐全、scorecard 是否 PASS、财务/审计签字角色是否完成。
- 已落地 `acceptedBaselineFailures` 门禁：工程证据齐全但 `PENDING_SIGNATURE` 时，不向 scorecard 提供“已接受差异”列表，避免未签字基线吞掉新增漂移；只有 `SIGNED` 且 `accepted=true` 后才可作为后续 baseline failure。
- 已生成 `assets/finance-signoff-baseline.md`：本地工程证据包为 `PASS`，签字状态明确为 `PENDING_SIGNATURE`，不伪造真实财务签字。
- 当前仍是 IN_PROGRESS：真实财务负责人/审计复核签字、live 双路取数证据、真实基线存储尚未完成。

## 验证

- [x] 本地财务签字基线报告成文，工程证据齐全且状态可机检
- [x] 每条证据含可第三方复跑命令；F5-T02 门禁 exit 0
- [x] 未签字基线不能作为 scorecard 已接受差异，签字基线才可压制重复漂移
- [ ] 财务签字基线报告经财务负责人/审计复核确认

## 完成标准

- [ ] 签字基线达成,IT 证据齐全、可重跑、非占位
