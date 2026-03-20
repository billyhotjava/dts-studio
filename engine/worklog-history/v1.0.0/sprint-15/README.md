# Sprint-15: Copilot 语音输入支持 (VI)

**前缀**: VI (Voice Input)
**状态**: READY
**目标**: 在 Copilot 聊天面板中支持语音输入，降低现场人员（养护人/项目经理）在移动端和桌面端的使用门槛。

## 背景

当前 Copilot 只支持文字输入。在实际业务场景中：

- **现场养护人**：在花卉养护现场双手不方便打字，语音询问"XX项目有多少绿植"更自然
- **项目经理巡查**：走动中快速查询项目数据，语音优于打字
- **移动端用户**：adminweb 嵌入的 Copilot 面板在手机上打字不便

语音输入将识别结果填入输入框，用户确认后发送，不改变现有的 chat 流程。

## 技术方案

### 分层架构

```
┌─ 前端 ─────────────────────────────────────────────────┐
│                                                        │
│  CopilotChat.tsx                                       │
│    └─ 输入区域                                          │
│         ├─ [+] 按钮                                     │
│         ├─ <textarea> 输入框                             │
│         ├─ 🎤 VoiceInputButton（新增）                   │
│         └─ 发送按钮                                      │
│                                                        │
│  VoiceInputButton.tsx                                   │
│    ├─ 状态机: idle → requesting → listening → processing │
│    ├─ 主引擎: Web Speech API (SpeechRecognition)        │
│    ├─ 降级引擎: MediaRecorder + 后端 ASR（可选）         │
│    └─ 输出: onTranscript(text) 回调                     │
│                                                        │
│  useVoiceInput.ts (hook)                                │
│    ├─ 封装 SpeechRecognition 生命周期                    │
│    ├─ 浏览器兼容检测                                     │
│    ├─ 权限管理（麦克风授权）                              │
│    └─ 错误处理和降级逻辑                                 │
│                                                        │
└────────────────────────────────────────────────────────┘
                        │ (可选：降级路径)
                        ▼
┌─ 后端（仅降级场景需要）──────────────────────────────────┐
│  POST /api/ai/voice/transcribe                          │
│    ├─ 接收 WebM/OGG 音频 blob                            │
│    ├─ 转发到配置的 ASR 服务（讯飞/阿里/本地 Whisper）      │
│    └─ 返回识别文本                                       │
└────────────────────────────────────────────────────────┘
```

### 主方案：Web Speech API

**核心逻辑：**

```typescript
// useVoiceInput.ts
function useVoiceInput(options: {
    lang?: string;              // 默认 'zh-CN'
    continuous?: boolean;       // 默认 false（单句模式）
    interimResults?: boolean;   // 默认 true（实时显示中间结果）
    onResult: (text: string, isFinal: boolean) => void;
    onError: (error: string) => void;
    onStateChange: (state: VoiceState) => void;
}) {
    // 返回: { start, stop, isSupported, state }
}

type VoiceState = 'idle' | 'requesting' | 'listening' | 'processing' | 'error';
```

**浏览器兼容性：**

| 浏览器 | 支持度 | 备注 |
|--------|--------|------|
| Chrome 33+ | 完全支持 | 使用 Google 语音服务 |
| Edge 79+ | 完全支持 | 使用 Microsoft 语音服务 |
| Safari 14.1+ | 部分支持 | iOS 需用户手势触发 |
| Firefox | 不支持 | 需后端降级方案 |
| 微信内置浏览器 | 不支持 | 需后端降级方案 |

**降级策略：**
1. 检测 `window.SpeechRecognition || window.webkitSpeechRecognition`
2. 不支持时：麦克风按钮变为录音模式（MediaRecorder），录音完成后发到后端 ASR
3. 后端 ASR 作为 P2 可选项，初版可以仅在不支持时隐藏按钮

### 交互设计

**状态流转：**

```
idle（麦克风图标灰色）
  │ 点击
  ▼
requesting（请求麦克风权限）
  │ 用户授权
  ▼
listening（麦克风图标红色 + 脉冲动画 + 波形指示器）
  │ 语音输入中，interim results 实时填入输入框
  │ 用户停顿 > 2s 或点击停止
  ▼
processing（处理中，短暂状态）
  │ final result 填入输入框
  ▼
idle（用户可编辑识别文本，确认后发送）
```

**UI 规范：**

- 麦克风按钮位置：输入框右侧，发送按钮左侧
- 尺寸：与发送按钮同高，24x24 图标
- 录音中：按钮变红，周围有脉冲光晕动画
- interim results：以浅色/斜体显示在输入框中，final 时变为正常样式
- 支持中途取消：录音中再次点击按钮取消
- 错误提示：权限被拒、网络错误等以 toast 形式显示

### 后端 ASR 降级方案（P2）

仅当浏览器不支持 Web Speech API 时使用：

```
POST /api/ai/voice/transcribe
Content-Type: multipart/form-data
Body: audio (WebM/OGG blob, max 30s)

Response: { "text": "识别出的文本", "confidence": 0.95 }
```

后端接入选项（按优先级）：
1. **阿里云语音识别**（已有通义千问 API Key，可能同账号开通）
2. **讯飞语音**（中文识别最优）
3. **本地 Whisper**（完全私有，但需要资源）

## 任务列表

| ID | 任务 | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| VI-01 | useVoiceInput Hook 实现 | P0 | READY | - |
| VI-02 | VoiceInputButton 组件 | P0 | READY | VI-01 |
| VI-03 | CopilotChat 集成 | P0 | READY | VI-02 |
| VI-04 | 移动端适配与手势处理 | P1 | READY | VI-03 |
| VI-05 | 后端 ASR 降级接口（可选） | P2 | READY | VI-03 |
| VI-06 | IT 测试与兼容性验证 | P2 | READY | VI-01~04 |

## 完成标准

- [ ] Chrome/Edge 桌面端：点击麦克风，说中文，识别文本填入输入框
- [ ] 录音状态有视觉反馈（红色脉冲）
- [ ] interim results 实时显示，final result 替换为最终文本
- [ ] 用户可编辑识别结果后再发送（不自动发送）
- [ ] 录音中可点击取消
- [ ] 浏览器不支持时麦克风按钮隐藏或显示提示
- [ ] 麦克风权限被拒时显示友好错误信息
- [ ] 不影响现有文字输入功能
- [ ] HTTPS 环境下正常工作
