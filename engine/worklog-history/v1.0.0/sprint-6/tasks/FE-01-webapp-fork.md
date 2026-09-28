# FE-01: dts-analytics-webapp fork 与品牌重构

**状态**: READY
**依赖**: BA-01

## 目标

fork dts-analytics-webapp 代码到 dts-copilot-webapp，更新品牌标识和项目配置。

## 技术设计

### 品牌重构

- 项目名: `dts-analytics-webapp` → `dts-copilot-webapp`
- 页面标题: 更新为 "DTS Copilot"
- Logo 和 favicon: 预留替换位
- package.json name: `@dts/copilot-webapp`

### 配置修改

```json
// package.json
{
  "name": "@dts/copilot-webapp",
  "version": "1.0.0",
  "scripts": {
    "dev": "vite --port 3003",
    "build": "vite build",
    "typecheck": "tsc --noEmit"
  }
}
```

### 路由保留

保持 `/analytics` 作为基础路由（与 dts-analytics-webapp 一致），方便迁移。

## 影响文件

- `dts-copilot-webapp/package.json`（修改）
- `dts-copilot-webapp/vite.config.ts`（修改：端口 3003）
- `dts-copilot-webapp/index.html`（修改：标题）
- `dts-copilot-webapp/src/i18n.ts`（修改：品牌文本）

## 完成标准

- [ ] `pnpm install` 依赖安装成功
- [ ] `pnpm dev` 开发服务器启动在 3003 端口
- [ ] 页面标题显示 "DTS Copilot"
- [ ] `pnpm build` 构建成功
