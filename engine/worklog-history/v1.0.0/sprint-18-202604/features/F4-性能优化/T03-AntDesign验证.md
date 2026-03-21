# T03: Ant Design tree-shaking 验证

**优先级**: P1
**状态**: READY
**依赖**: F1/T02

## 目标

验证 Ant Design v5 的 tree-shaking 是否生效，确保只打包使用的组件。

## 技术设计

Ant Design v5 默认支持 tree-shaking（ES module 导出），但需要确认：

1. 导入方式正确：`import { Button } from 'antd'`（非 `import antd from 'antd'`）
2. 图标按需导入：`import { SearchOutlined } from '@ant-design/icons'`
3. 没有全量导入 CSS（antd v5 用 CSS-in-JS，无需 import CSS）

### 检查步骤

```bash
# 搜索是否有全量导入
grep -r "import antd" src/ --include="*.tsx" --include="*.ts"
grep -r "import \* as antd" src/ --include="*.tsx" --include="*.ts"
grep -r "from 'antd'" src/ --include="*.tsx" --include="*.ts" | head -20

# 搜索图标导入方式
grep -r "@ant-design/icons" src/ --include="*.tsx" | head -20
```

### bundle 报告验证

在 bundle 分析报告中确认 antd chunk 体积合理（< 500KB ungzipped）。

## 完成标准

- [ ] 无全量导入 antd 的代码
- [ ] 图标按需导入
- [ ] bundle 报告中 antd 体积 < 500KB
