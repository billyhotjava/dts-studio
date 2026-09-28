# T03: 清理 polyfill 和兼容代码

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

移除不再需要的 polyfill 和浏览器兼容 shim。

## 技术设计

### 1. 移除 @ungap/structured-clone

Chrome 98+ 原生支持 `structuredClone()`，不再需要 polyfill。

```bash
pnpm remove @ungap/structured-clone
```

在代码中搜索引用并替换为原生 `structuredClone()`：

```bash
grep -r "structured-clone\|structuredClone" src/
```

### 2. 检查 src/polyfills/ 目录

```bash
ls src/polyfills/
```

评估每个 polyfill 是否在 Chrome 120+ 中仍需要，不需要的移除。

### 3. 清理 vendor prefixes

以下 CSS vendor prefixes 在 Chrome 120+ 中不再需要：
- ~~`-webkit-backdrop-filter`~~ → `backdrop-filter`（Chrome 76+ 无需前缀）

保留仍需要的：
- `-webkit-scrollbar` 系列（Chrome 自定义滚动条仍需要）
- `-webkit-line-clamp`（仍需要 webkit 前缀）

## 完成标准

- [ ] `@ungap/structured-clone` 从 package.json 移除
- [ ] 代码中无该 polyfill 的 import
- [ ] src/polyfills/ 下不需要的文件移除
- [ ] CSS 中不需要的 vendor prefix 清理
