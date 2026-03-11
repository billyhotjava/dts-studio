# Sprint 1: prs-stack submodule normalization

目标：
- 规范化 `app-stack/prs-stack`
- 让当前已存在的内嵌仓库和 `app-stack` 索引、`.gitmodules` 完全一致

任务顺序：
1. [PS-01-verify-prs-repo.md](/opt/prod/dts/rdc/worklog/app-stack/prs-stack/sprint-1-submodule-normalization/PS-01-verify-prs-repo.md)
2. [PS-02-register-prs-submodule.md](/opt/prod/dts/rdc/worklog/app-stack/prs-stack/sprint-1-submodule-normalization/PS-02-register-prs-submodule.md)
3. [PS-03-verify-prs-submodule.md](/opt/prod/dts/rdc/worklog/app-stack/prs-stack/sprint-1-submodule-normalization/PS-03-verify-prs-submodule.md)

完成标准：
- `prs-stack` 独立仓库已绑定 `git@github.com:billyhotjava/prs-stack.git`
- `app-stack/.gitmodules` 包含 `prs-stack`
- `git -C app-stack submodule status` 能正确显示 `prs-stack`
