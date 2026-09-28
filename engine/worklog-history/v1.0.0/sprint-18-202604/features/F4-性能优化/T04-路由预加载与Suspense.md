# T04: 路由预加载与 Suspense 优化

**优先级**: P1
**状态**: READY
**依赖**: 无

## 目标

优化路由切换体验：加 Suspense fallback、鼠标 hover 时预加载目标路由。

## 技术设计

### 1. Suspense fallback

当前路由懒加载可能在慢网下显示白屏。加 Suspense fallback：

```tsx
// routes.tsx
<Suspense fallback={<RouteLoadingIndicator />}>
    <Outlet />
</Suspense>
```

`RouteLoadingIndicator` 显示一个轻量的加载动画（不用 antd Spin，纯 CSS）。

### 2. 路由预加载

```tsx
// NavLink hover 时预加载
function PrefetchLink({ to, children, ...props }) {
    const handleMouseEnter = () => {
        // 触发 route 的 lazy import
        const route = routeMap.get(to);
        if (route?.lazy) route.lazy();
    };

    return (
        <Link to={to} onMouseEnter={handleMouseEnter} {...props}>
            {children}
        </Link>
    );
}
```

## 完成标准

- [ ] 路由切换时有 loading fallback（非白屏）
- [ ] 导航菜单 hover 时预加载目标路由
- [ ] loading 动画纯 CSS（不增加 bundle）
