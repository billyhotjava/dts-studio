# PS-01 Verify prs repo

目标：
- 确认 `prs-stack` 独立仓库状态完整可用，并已对接远端

前置条件：
- `app-stack/prs-stack` 当前工作树无未处理冲突

执行步骤：
1. 运行 `git -C app-stack/prs-stack status`
2. 运行 `git -C app-stack/prs-stack remote -v`
3. 如未配置远端，执行 `git -C app-stack/prs-stack remote add origin git@github.com:billyhotjava/prs-stack.git`
4. 运行 `git -C app-stack/prs-stack branch -M main`
5. 运行 `git -C app-stack/prs-stack push -u origin main`

验证：
- `git -C app-stack/prs-stack remote -v` 指向 `git@github.com:billyhotjava/prs-stack.git`
- `git -C app-stack/prs-stack branch --show-current` 为 `main`
- `git -C app-stack/prs-stack log --oneline -n 1` 的提交在远端可达

输出：
- `prs-stack` 的远端与分支基线已经就位
