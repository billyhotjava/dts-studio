<template>
  <div class="copilot-container">
    <iframe
      ref="copilotFrame"
      :src="copilotUrl"
      class="copilot-iframe"
      @load="onIframeLoad"
    />
  </div>
</template>

<script>
/**
 * 园林平台 AdminWeb 嵌入 DTS Copilot 示例组件
 *
 * 使用说明：
 * 1. 将此文件复制到 adminweb/src/views/copilot/chat.vue
 * 2. 在 router 中添加路由（参考 adminweb-menu.sql 中的菜单配置）
 * 3. 配置 .env 中的 VUE_APP_COPILOT_URL（可选，默认走 gateway 代理）
 *
 * 依赖：
 * - @/utils/auth 中的 getToken 方法（获取园林平台 JWT）
 * - vuex store 中的 user 模块（获取当前用户信息）
 */
import { getToken } from '@/utils/auth'
import { mapGetters } from 'vuex'

export default {
  name: 'CopilotView',
  computed: {
    ...mapGetters(['userId', 'name', 'nickName']),
    copilotUrl() {
      // 默认通过 gateway 代理访问，也可配置直连地址
      const base = process.env.VUE_APP_COPILOT_URL || '/copilot/web'
      return `${base}?embed=true`
    }
  },
  methods: {
    onIframeLoad() {
      // 通过 postMessage 传递认证信息给 copilot webapp
      // copilot webapp 收到后会使用这些信息标识用户
      this.$refs.copilotFrame.contentWindow.postMessage({
        type: 'SET_AUTH',
        userId: this.userId,
        userName: this.name,
        displayName: this.nickName
      }, '*')
    }
  }
}
</script>

<style scoped>
.copilot-container {
  height: calc(100vh - 84px);
  width: 100%;
}
.copilot-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
