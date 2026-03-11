# PS-03 Verify prs-stack submodule

目标：
- 验证 `prs-stack` submodule 状态、配置和指针都一致

执行步骤：
1. 运行 `git -C app-stack status`
2. 运行 `git -C app-stack submodule status`
3. 运行 `git -C app-stack config -f .gitmodules --get-regexp '^submodule\\.'`
4. 运行 `git -C app-stack/prs-stack remote -v`
5. 如有条件，在新目录执行 `git clone --recurse-submodules <app-stack-url>`

验证：
- `git -C app-stack submodule status` 包含 `prs-stack`
- `.gitmodules` 中 `prs-stack` 的 path 与 url 正确
- 新克隆仓库能正常初始化 `prs-stack`

输出：
- `prs-stack` submodule 规范化验证完成
