# F6 单窗口结果面收口验证

**时间**: 2026-05-31
**结论**: PASS

## 变更验证

目标:取消 Agent BI 工作台右侧 `CanvasPanel`,恢复对话窗口内 inline SQL 自动预览为唯一结果面。

## 命令

```bash
cd dts-copilot-webapp
pnpm test -- src/pages/AgentWorkspacePage.test.ts src/components/copilot/MessageList.inlinePreview.test.tsx
pnpm typecheck
pnpm build
```

结果:

- Vitest 实际运行 61 个测试文件,247 个测试全部通过。
- `pnpm typecheck` 通过。
- `pnpm build` 通过,仅保留既有 chunk size warning。

## 容器验证

```bash
docker compose build copilot-webapp
docker compose up -d --force-recreate --no-deps copilot-webapp
docker compose ps copilot-webapp
curl -fsS -o /dev/null -w '%{http_code}\n' http://localhost:50080/agent-bi
docker exec dts-copilot-webapp sh -lc 'grep -R "agent-workspace__canvas\|CanvasPanel" -n /usr/share/nginx/html/assets/AgentWorkspacePage-* 2>/dev/null || true'
```

结果:

- 新镜像: `sha256:ca3976554d34e6c3ae4defa9c0aeb2fd7cfc0527952c3b9c19243d9f3ab10b1c`
- `dts-copilot-webapp` 状态: healthy
- `/agent-bi` HTTP 状态: 200
- 容器内 AgentWorkspacePage 构建产物 grep 无 `agent-workspace__canvas` / `CanvasPanel` 输出。
