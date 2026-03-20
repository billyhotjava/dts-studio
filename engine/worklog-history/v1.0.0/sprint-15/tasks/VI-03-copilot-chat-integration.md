# VI-03: CopilotChat 集成

**优先级**: P0
**状态**: READY
**依赖**: VI-02

## 目标

将 VoiceInputButton 集成到 CopilotChat 的输入区域，处理识别文本与输入框的交互。

## 技术设计

### 集成位置

在 `CopilotChat.tsx` 的输入区域，textarea 和发送按钮之间插入 VoiceInputButton：

```
现有布局:  [+] [________textarea________] [发送]
新布局:    [+] [________textarea________] [🎤] [发送]
```

### 代码改动

```tsx
// CopilotChat.tsx 输入区域

// 新增 state：追踪语音输入的 interim text
const [voiceInterimText, setVoiceInterimText] = useState('');

// 语音识别回调
const handleVoiceTranscript = useCallback((text: string, isFinal: boolean) => {
    if (isFinal) {
        // Final result: 替换输入框内容（清除 interim）
        setInput(text);
        setVoiceInterimText('');
    } else {
        // Interim result: 临时显示，不覆盖已有输入
        setVoiceInterimText(text);
    }
}, []);

// 输入框显示逻辑：如果有 voiceInterimText，追加显示
// textarea 的 value = input + (voiceInterimText ? voiceInterimText : '')
// 或者更简单：interim 时直接设置 input，final 时确认

// 在 textarea 和发送按钮之间：
<VoiceInputButton
    onTranscript={handleVoiceTranscript}
    disabled={!canEditComposer || sending}
/>
```

### 交互细节

1. **识别中不自动发送**：识别完成后文本填入输入框，用户需手动点发送或按回车
2. **与手动输入不冲突**：如果用户已经输入了一些文字，语音识别的文本追加到后面
3. **interim text 样式**：通过 CSS 让 interim 部分显示为浅色（可选，初版可以直接替换）
4. **多次语音**：每次点击麦克风是一次独立的识别周期，结果追加到输入框

### CSS 调整

```css
/* 输入区域 flex 布局调整 */
.copilot-chat__input-row {
    display: flex;
    align-items: flex-end;
    gap: var(--spacing-xs);
}

/* 麦克风按钮与发送按钮对齐 */
.voice-input-btn {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    /* ... */
}
```

## 完成标准

- [ ] 麦克风按钮出现在输入框和发送按钮之间
- [ ] 点击麦克风 → 说话 → 文本出现在输入框
- [ ] 用户可以编辑识别结果后再发送
- [ ] 语音输入不影响手动打字
- [ ] sending 状态下麦克风禁用
- [ ] 布局在 sidebar 窄屏下不溢出
