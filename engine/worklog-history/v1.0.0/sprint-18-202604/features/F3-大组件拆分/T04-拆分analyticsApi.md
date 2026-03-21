# T04: 拆分 analyticsApi.ts

**优先级**: P1
**状态**: READY
**依赖**: 无

## 目标

将 analyticsApi.ts (2,490 行) 按业务域拆分为多个模块。

## 拆分方案

```
api/
├── analyticsApi.ts            # 入口（re-export 所有模块）~100 行
├── types.ts                   # 所有类型定义 ~500 行
├── httpClient.ts              # fetch 封装、认证、错误处理 ~200 行
├── modules/
│   ├── auth.ts                # 登录/会话/用户 ~200 行
│   ├── database.ts            # 数据源/数据库/表 ~200 行
│   ├── card.ts                # 卡片/查询 ~200 行
│   ├── dashboard.ts           # 仪表盘 ~150 行
│   ├── screen.ts              # 大屏相关 ~300 行
│   ├── copilot.ts             # AI Copilot 相关 ~200 行
│   └── admin.ts               # 管理/配置 ~200 行
└── compat.ts                  # 旧 API 兼容层 ~200 行
```

### 迁移策略

保持 `analyticsApi` 对象的接口不变，只改内部组织：

```typescript
// analyticsApi.ts (入口)
export { analyticsApi } from './modules';
export type { ... } from './types';
```

## 完成标准

- [ ] 每个模块文件 < 500 行
- [ ] 现有 import { analyticsApi } 无需修改
- [ ] 类型导出完整
