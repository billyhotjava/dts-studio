# T04: 清理冗余 CSS 和死代码

**优先级**: P1
**状态**: READY
**依赖**: T02

## 目标

使用工具扫描未使用的 CSS 类名，清理死代码。

## 技术设计

### 工具选择

使用 `purgecss` 或手动 grep 扫描：

```bash
# 从 CSS 提取所有类名
grep -ohP '\.\w[\w-]+' src/**/*.css | sort -u > /tmp/css-classes.txt

# 从 TSX/TS 提取引用的类名
grep -ohP '(?:className|class)="[^"]*"' src/**/*.tsx | sort -u > /tmp/used-classes.txt

# 对比差异
comm -23 /tmp/css-classes.txt /tmp/used-classes.txt
```

### 重点清理

1. 不再使用的 vendor prefix（F1/T03 已识别）
2. 被删除组件遗留的样式
3. utilities.css 中未使用的工具类

## 完成标准

- [ ] 未使用的 CSS 规则清理完毕
- [ ] CSS 总行数在嵌套改写基础上再减少 10%
