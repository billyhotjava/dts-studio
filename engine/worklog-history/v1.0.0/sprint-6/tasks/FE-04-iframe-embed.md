# FE-04: iframe 嵌入模式支持

**状态**: READY
**依赖**: FE-03

## 目标

支持 copilot-webapp 通过 iframe 嵌入到业务系统（如园林平台 adminweb），提供无缝的用户体验。

## 技术设计

### 嵌入模式检测

```typescript
// URL 参数: ?embed=true&apiKey=cpk_xxx&userId=user001
const isEmbedMode = new URLSearchParams(location.search).has('embed')
```

### 嵌入模式下的 UI 调整

- 隐藏顶部导航栏
- 隐藏侧边栏（如果不需要）
- 全屏展示内容区域
- 通过 postMessage 与父窗口通信

### postMessage API

```typescript
// 父窗口 → iframe
window.postMessage({ type: 'SET_API_KEY', apiKey: 'cpk_xxx' })
window.postMessage({ type: 'SET_USER', userId: 'user001', userName: '张三' })
window.postMessage({ type: 'NAVIGATE', path: '/analytics/dashboards/1' })

// iframe → 父窗口
parent.postMessage({ type: 'COPILOT_READY' })
parent.postMessage({ type: 'NAVIGATION_CHANGE', path: '/analytics/...' })
```

### CSP 与跨域

```
X-Frame-Options: ALLOWALL
Content-Security-Policy: frame-ancestors *;
```

## 影响文件

- `dts-copilot-webapp/src/hooks/useEmbedMode.ts`（新建）
- `dts-copilot-webapp/src/layouts/EmbedLayout.tsx`（新建）
- `dts-copilot-webapp/src/routes.tsx`（修改：根据 embed 模式切换 layout）

## 完成标准

- [ ] `?embed=true` 模式下隐藏导航栏
- [ ] postMessage 通信正常
- [ ] 在 adminweb iframe 中可正常展示和交互
- [ ] 无跨域错误
