# T03: 拆分 ScreenHeader.tsx

**优先级**: P1
**状态**: READY
**依赖**: 无

## 目标

将 ScreenHeader.tsx (2,617 行) 拆分为多个文件。

## 拆分方案

```
ScreenHeader/
├── index.tsx                  # 主头部栏 ~300 行
├── ScreenToolbar.tsx          # 工具栏按钮组 ~400 行
├── ScreenExportDialog.tsx     # 导出对话框 ~300 行
├── ScreenShareDialog.tsx      # 分享对话框 ~300 行
├── ScreenVersionPanel.tsx     # 版本管理面板 ~400 行
├── hooks/
│   └── useScreenActions.ts    # 操作逻辑（保存/发布/导出）~400 行
└── constants.ts               # 工具栏配置常量 ~200 行
```

## 完成标准

- [ ] 每个文件 < 500 行
- [ ] 大屏编辑器头部功能正常
