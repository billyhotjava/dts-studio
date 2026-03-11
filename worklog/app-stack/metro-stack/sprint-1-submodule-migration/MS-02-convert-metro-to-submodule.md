# MS-02 Convert metro-stack to submodule

目标：
- 把 `metro-stack` 从 `app-stack` 当前普通跟踪目录迁成正式 submodule

前置条件：
- [MS-01-bootstrap-metro-repo.md](/opt/prod/dts/rdc/worklog/app-stack/metro-stack/sprint-1-submodule-migration/MS-01-bootstrap-metro-repo.md) 已完成
- `metro-stack` 当前提交已推送远端

执行步骤：
1. 在 `app-stack` 中确认 `metro-stack` 仍是普通文件跟踪状态：`git -C app-stack ls-files --stage metro-stack | head`
2. 记录当前 `metro-stack` 目标提交：`git -C app-stack/metro-stack rev-parse HEAD`
3. 从 `app-stack` 索引移除 `metro-stack`：`git -C app-stack rm -r --cached metro-stack`
4. 清理工作树目录占位，保留可恢复副本后重新挂载 submodule
5. 运行 `git -C app-stack submodule add git@github.com:billyhotjava/metro-stack.git metro-stack`
6. 如果 submodule 检出提交不是目标提交，在 `app-stack/metro-stack` 内切到目标提交
7. 回到 `app-stack`，提交 `.gitmodules` 与 submodule 指针变更

验证：
- `git -C app-stack config -f .gitmodules --get submodule.metro-stack.path` 返回 `metro-stack`
- `git -C app-stack config -f .gitmodules --get submodule.metro-stack.url` 返回 `git@github.com:billyhotjava/metro-stack.git`
- `git -C app-stack ls-files --stage metro-stack` 显示 mode `160000`

输出：
- `app-stack` 不再直接跟踪 `metro-stack` 内部普通文件
- `metro-stack` 成为正式 submodule
