# MS-03 Verify metro-stack submodule

目标：
- 验证 `metro-stack` 迁移后的状态可持续使用

执行步骤：
1. 运行 `git -C app-stack status`
2. 运行 `git -C app-stack submodule status`
3. 运行 `git -C app-stack config -f .gitmodules --get-regexp '^submodule\\.'`
4. 运行 `git -C app-stack/metro-stack remote -v`
5. 如有条件，在新目录执行 `git clone --recurse-submodules <app-stack-url>`

验证：
- `git -C app-stack status` 只包含预期改动
- `git -C app-stack submodule status` 包含 `metro-stack`
- 新克隆仓库能正常初始化 `metro-stack`

输出：
- `metro-stack` submodule 迁移验证完成，可进入后续提交或 PR 阶段
