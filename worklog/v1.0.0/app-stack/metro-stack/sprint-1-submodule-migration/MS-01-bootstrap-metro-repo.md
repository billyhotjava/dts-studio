# MS-01 Bootstrap metro repo

目标：
- 在 `app-stack/metro-stack` 内建立独立 Git 仓库
- 把当前目录内容提交并推送到远端 `git@github.com:billyhotjava/metro-stack.git`

前置条件：
- `app-stack/metro-stack` 当前内容已确认要整体保留
- 本机有 GitHub SSH 推送权限

执行步骤：
1. 进入 `app-stack/metro-stack`
2. 运行 `git init`
3. 运行 `git add .`
4. 运行 `git commit -m "chore: bootstrap metro-stack repo"`
5. 运行 `git remote add origin git@github.com:billyhotjava/metro-stack.git`
6. 运行 `git branch -M main`
7. 运行 `git push -u origin main`

验证：
- `git -C app-stack/metro-stack status` 为空工作区
- `git -C app-stack/metro-stack remote -v` 指向 `git@github.com:billyhotjava/metro-stack.git`
- `git -C app-stack/metro-stack branch --show-current` 为 `main`

输出：
- `metro-stack` 有自己的仓库身份，可被 `app-stack` 以 submodule 方式引用
