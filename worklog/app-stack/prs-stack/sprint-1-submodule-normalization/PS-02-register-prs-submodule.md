# PS-02 Register prs-stack submodule

目标：
- 把当前 `app-stack` 中已经存在的 `prs-stack` gitlink 补齐为正式 submodule

前置条件：
- [PS-01-verify-prs-repo.md](/opt/prod/dts/rdc/worklog/app-stack/prs-stack/sprint-1-submodule-normalization/PS-01-verify-prs-repo.md) 已完成
- `git -C app-stack ls-files --stage prs-stack` 已显示 mode `160000`

执行步骤：
1. 在 `app-stack/.gitmodules` 增加 `prs-stack` 映射
2. 写入：
   - `submodule.prs-stack.path=prs-stack`
   - `submodule.prs-stack.url=git@github.com:billyhotjava/prs-stack.git`
3. 运行 `git -C app-stack submodule sync -- prs-stack`
4. 运行 `git -C app-stack submodule init prs-stack`
5. 如有必要，运行 `git -C app-stack submodule update --checkout prs-stack`
6. 在 `app-stack` 提交 `.gitmodules` 修正

验证：
- `git -C app-stack config -f .gitmodules --get submodule.prs-stack.path` 返回 `prs-stack`
- `git -C app-stack config -f .gitmodules --get submodule.prs-stack.url` 返回 `git@github.com:billyhotjava/prs-stack.git`
- `git -C app-stack submodule status` 能显示 `prs-stack`

输出：
- `prs-stack` 从“半成品 gitlink”收敛为正式 submodule
