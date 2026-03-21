# T01: 启用 CSS 原生嵌套

**优先级**: P0
**状态**: READY
**依赖**: 无

## 目标

确认 Vite 构建链支持 CSS 原生嵌套语法（Chrome 120+ 原生支持，无需 PostCSS 插件）。

## 技术设计

CSS 原生嵌套已在 Chrome 120+、Edge 120+ 中完全支持。由于我们的构建目标已升级（F1/T01），可以直接使用原生语法。

### 验证方式

创建一个测试 CSS 文件确认 Vite 不会报错：

```css
.parent {
    color: red;

    & .child {
        color: blue;
    }

    &:hover {
        color: green;
    }

    @media (width >= 768px) {
        font-size: 16px;
    }
}
```

### 注意事项

- Vite 6 内置的 CSS 处理已支持原生嵌套透传
- 不需要安装 `postcss-nesting` 插件
- 确认 `.css` 文件中嵌套语法不被 esbuild 意外转译

## 完成标准

- [ ] 嵌套 CSS 在 dev 和 build 模式下正常工作
- [ ] 无 PostCSS 转译（原生透传到浏览器）
