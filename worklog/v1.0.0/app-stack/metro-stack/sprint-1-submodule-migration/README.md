# Sprint 1: metro-stack submodule migration

目标：
- 把 `app-stack/metro-stack` 从普通目录迁移为独立 Git 仓库
- 推送到 `git@github.com:billyhotjava/metro-stack.git`
- 让 `app-stack` 以正式 submodule 方式引用该目录

任务顺序：
1. [MS-01-bootstrap-metro-repo.md](/opt/prod/dts/rdc/worklog/app-stack/metro-stack/sprint-1-submodule-migration/MS-01-bootstrap-metro-repo.md)
2. [MS-02-convert-metro-to-submodule.md](/opt/prod/dts/rdc/worklog/app-stack/metro-stack/sprint-1-submodule-migration/MS-02-convert-metro-to-submodule.md)
3. [MS-03-verify-metro-submodule.md](/opt/prod/dts/rdc/worklog/app-stack/metro-stack/sprint-1-submodule-migration/MS-03-verify-metro-submodule.md)

完成标准：
- `metro-stack` 有独立 Git 历史、`origin` 指向 GitHub、默认分支为 `main`
- `app-stack/.gitmodules` 包含 `metro-stack`
- `git -C app-stack submodule status` 能正确显示 `metro-stack`
