# IT00 Sprint-28 基线

## 范围

记录 Sprint-28 收口后的构建基线。原始诊断中的“typecheck/test/build 全绿”在本轮最终验证中重新执行确认。

## 验证命令

```bash
cd dts-copilot-webapp
pnpm typecheck
pnpm test
pnpm build

cd ..
mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test
```

## 结果

- `pnpm typecheck`: exit 0。
- `pnpm test`: 60 个 test files / 244 个 tests 全部通过。
- `pnpm build`: exit 0;保留既有 `vendor-echarts` 等大 chunk warning。
- `mvn -pl dts-copilot-ai -Dtest=AiCopilotResourceTest test`: 2 个 tests 全部通过。

## 说明

`node --test tests/*.test.ts` 不是当前 webapp 全量闸门;该命令包含多个历史 `.js` import 和已删除旧路由断言,失败与 Sprint-28 新增改动无直接关系。本轮新增/修改的 `tests/appShellConfig.test.ts` 已单独执行并通过。
