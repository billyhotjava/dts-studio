# T02: Biome 规则收紧

**优先级**: P1
**状态**: READY
**依赖**: 无

## 目标

收紧 Biome lint 规则，启用更多现代最佳实践检查。

## 技术设计

检查当前 `biome.json` 配置，启用以下规则：

```json
{
    "linter": {
        "rules": {
            "style": {
                "noNonNullAssertion": "warn",
                "useConst": "error",
                "useExponentiationOperator": "error",
                "useTemplate": "warn"
            },
            "suspicious": {
                "noExplicitAny": "warn",
                "noConsoleLog": "warn"
            },
            "complexity": {
                "noForEach": "warn",
                "useFlatMap": "error"
            },
            "performance": {
                "noAccumulatingSpread": "warn"
            }
        }
    }
}
```

## 完成标准

- [ ] Biome 规则更新
- [ ] `pnpm lint` 零 error
- [ ] warn 级别问题记录但不阻塞
