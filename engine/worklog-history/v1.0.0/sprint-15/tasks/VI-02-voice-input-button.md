# VI-02: VoiceInputButton 组件

**优先级**: P0
**状态**: READY
**依赖**: VI-01

## 目标

实现麦克风按钮组件，包含录音状态动画、错误提示和交互反馈。

## 技术设计

### 文件

- `dts-copilot-webapp/src/components/copilot/VoiceInputButton.tsx`
- `dts-copilot-webapp/src/components/copilot/VoiceInputButton.css`

### Props

```typescript
interface VoiceInputButtonProps {
    /** 识别到的文本回调（interim 和 final 都会触发） */
    onTranscript: (text: string, isFinal: boolean) => void;
    /** 是否禁用 */
    disabled?: boolean;
}
```

### 组件结构

```tsx
function VoiceInputButton({ onTranscript, disabled }: VoiceInputButtonProps) {
    const { start, stop, state, isSupported, errorMessage } = useVoiceInput({
        lang: 'zh-CN',
        continuous: false,
        interimResults: true,
        onResult: onTranscript,
    });

    if (!isSupported) return null; // 不支持时不渲染

    const handleClick = () => {
        if (state === 'listening') {
            stop();
        } else {
            start();
        }
    };

    return (
        <div className="voice-input-wrapper">
            <button
                type="button"
                className={`voice-input-btn voice-input-btn--${state}`}
                onClick={handleClick}
                disabled={disabled || state === 'requesting' || state === 'processing'}
                title={state === 'listening' ? '点击停止录音' : '点击开始语音输入'}
                aria-label={state === 'listening' ? '停止录音' : '语音输入'}
            >
                {/* 麦克风 SVG 图标 */}
                <MicIcon />
                {/* 录音中的脉冲动画 */}
                {state === 'listening' && <span className="voice-input-pulse" />}
            </button>
            {/* 错误提示 toast */}
            {state === 'error' && errorMessage && (
                <div className="voice-input-error">{errorMessage}</div>
            )}
        </div>
    );
}
```

### 视觉设计

**idle 状态：**
- 麦克风图标，颜色与发送按钮一致（`var(--color-text-secondary)`）
- hover 时变亮

**listening 状态：**
- 图标变红 `#ef4444`
- 外圈脉冲动画（两层 ring 交替扩散）
- CSS animation:

```css
.voice-input-btn--listening {
    color: #ef4444;
}

.voice-input-pulse {
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 2px solid #ef4444;
    animation: voice-pulse 1.5s ease-out infinite;
}

@keyframes voice-pulse {
    0% { transform: scale(1); opacity: 0.8; }
    100% { transform: scale(1.6); opacity: 0; }
}
```

**error 状态：**
- 图标变黄，3 秒后恢复
- 下方弹出错误 toast，自动消失

**MicIcon：** 纯 SVG，不依赖图标库：

```tsx
function MicIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="9" y="1" width="6" height="11" rx="3" />
            <path d="M19 10v1a7 7 0 0 1-14 0v-1" />
            <line x1="12" y1="19" x2="12" y2="23" />
            <line x1="8" y1="23" x2="16" y2="23" />
        </svg>
    );
}
```

## 完成标准

- [ ] 麦克风按钮渲染正确，与输入框风格一致
- [ ] 点击开始录音，再次点击停止
- [ ] 录音中有红色脉冲动画
- [ ] 错误时显示中文提示，3 秒自动消失
- [ ] 浏览器不支持时按钮不渲染
- [ ] disabled 时按钮灰色不可点击
- [ ] 无外部依赖（SVG 图标内联）
