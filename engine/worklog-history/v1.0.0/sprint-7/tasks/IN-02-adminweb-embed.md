# IN-02: adminweb 嵌入 copilot-webapp

**状态**: READY
**依赖**: FE-04

## 目标

在园林平台 adminweb 中通过 iframe 嵌入 copilot-webapp，作为菜单项供用户使用。

## 技术设计

### 菜单配置

在园林平台后端菜单管理中添加：

| 菜单名 | 路径 | 类型 |
|--------|------|------|
| AI 助手 | /copilot/chat | 内链 iframe |
| 数据分析 | /copilot/dashboard | 内链 iframe |
| 报表中心 | /copilot/report | 内链 iframe |

### iframe 集成

```vue
<!-- adminweb/src/views/copilot/index.vue -->
<template>
  <iframe
    :src="copilotUrl"
    style="width: 100%; height: calc(100vh - 84px); border: none;"
    @load="onIframeLoad"
  />
</template>

<script>
export default {
  computed: {
    copilotUrl() {
      const base = process.env.VUE_APP_COPILOT_URL || '/copilot/web'
      const token = this.$store.getters.token
      return `${base}/analytics?embed=true&token=${token}`
    }
  }
}
</script>
```

### Token 传递

方案一：URL 参数传递（简单但不安全）
方案二：postMessage 传递（推荐）

```javascript
// adminweb 父页面
iframe.contentWindow.postMessage({
  type: 'SET_AUTH',
  apiKey: 'cpk_xxx',
  userId: this.$store.getters.userId,
  userName: this.$store.getters.name
}, '*')
```

## 影响文件

- `adminweb/src/views/copilot/index.vue`（新建）
- `adminweb/src/views/copilot/chat.vue`（新建）
- `adminweb/src/views/copilot/dashboard.vue`（新建）
- 后端菜单数据（SQL 或 API）

## 完成标准

- [ ] 园林平台菜单中可见 "AI 助手" 和 "数据分析" 入口
- [ ] 点击打开 iframe 嵌入的 copilot-webapp
- [ ] 认证信息正确传递，无需二次登录
- [ ] iframe 内可正常操作 BI 功能和 AI 对话
