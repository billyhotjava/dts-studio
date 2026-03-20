# VI-04: 移动端适配与手势处理

**优先级**: P1
**状态**: READY
**依赖**: VI-03

## 目标

确保语音输入在移动端（特别是 iOS Safari 和微信内置浏览器）正常工作或优雅降级。

## 技术设计

### iOS Safari 特殊处理

iOS Safari 的 Web Speech API 有以下限制：
1. **必须由用户手势触发**（click 事件内调用 `.start()`）
2. **不支持 `continuous = true`**
3. **可能在后台时停止**

处理方案：
```typescript
// useVoiceInput.ts 中检测 iOS
const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);

// iOS 强制 continuous = false
if (isIOS) {
    recognition.continuous = false;
}
```

### 微信内置浏览器

微信浏览器不支持 Web Speech API，需要：
1. 检测 `isSupported = false` → 隐藏麦克风按钮
2. 或者后续通过 VI-05（后端 ASR）提供降级

检测微信浏览器：
```typescript
const isWechat = /MicroMessenger/i.test(navigator.userAgent);
```

### 触摸设备优化

- 按钮尺寸在触摸设备上增大到 44x44（Apple HIG 最小触摸目标）
- 长按提示改为 tap 提示
- 录音中防止误触发送按钮

```css
@media (pointer: coarse) {
    .voice-input-btn {
        width: 44px;
        height: 44px;
    }
}
```

### 权限引导

移动端首次使用时，浏览器会弹出麦克风权限请求：
- 在点击麦克风按钮前，检查 `navigator.permissions.query({ name: 'microphone' })`
- 如果是 `denied` 状态，直接显示"请在浏览器设置中允许麦克风"
- 如果是 `prompt` 状态，正常弹窗

## 完成标准

- [ ] iOS Safari：语音输入可用（由 click 触发，非 continuous）
- [ ] Android Chrome：语音输入正常工作
- [ ] 微信浏览器：麦克风按钮隐藏，不报错
- [ ] 触摸设备按钮尺寸 >= 44px
- [ ] 权限被拒后显示友好引导
