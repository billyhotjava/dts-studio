# VI-01: useVoiceInput Hook 实现

**优先级**: P0
**状态**: READY
**依赖**: -

## 目标

封装 Web Speech API 为 React Hook，处理浏览器兼容性、权限管理、状态机和错误恢复。

## 技术设计

### 文件位置

`dts-copilot-webapp/src/hooks/useVoiceInput.ts`

### 接口定义

```typescript
export type VoiceState = 'idle' | 'requesting' | 'listening' | 'processing' | 'error';

export interface UseVoiceInputOptions {
    /** 识别语言，默认 'zh-CN' */
    lang?: string;
    /** 是否连续识别，默认 false（单句模式，停顿后自动结束） */
    continuous?: boolean;
    /** 是否返回中间结果，默认 true */
    interimResults?: boolean;
    /** 最大录音时长（秒），默认 30，超时自动停止 */
    maxDuration?: number;
    /** 识别结果回调 */
    onResult: (text: string, isFinal: boolean) => void;
    /** 错误回调 */
    onError?: (error: string) => void;
}

export interface UseVoiceInputReturn {
    /** 开始语音识别 */
    start: () => void;
    /** 停止语音识别 */
    stop: () => void;
    /** 当前状态 */
    state: VoiceState;
    /** 浏览器是否支持 Web Speech API */
    isSupported: boolean;
    /** 错误信息（state='error' 时有值） */
    errorMessage: string | null;
}
```

### 核心实现逻辑

```typescript
export function useVoiceInput(options: UseVoiceInputOptions): UseVoiceInputReturn {
    const [state, setState] = useState<VoiceState>('idle');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const recognitionRef = useRef<SpeechRecognition | null>(null);
    const timeoutRef = useRef<number | null>(null);

    // 浏览器兼容检测
    const isSupported = useMemo(() => {
        return typeof window !== 'undefined' &&
            ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window);
    }, []);

    const start = useCallback(() => {
        if (!isSupported || state === 'listening') return;

        setState('requesting');

        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        const recognition = new SpeechRecognition();

        recognition.lang = options.lang ?? 'zh-CN';
        recognition.continuous = options.continuous ?? false;
        recognition.interimResults = options.interimResults ?? true;
        recognition.maxAlternatives = 1;

        recognition.onstart = () => setState('listening');

        recognition.onresult = (event) => {
            let interimText = '';
            let finalText = '';
            for (let i = event.resultIndex; i < event.results.length; i++) {
                const transcript = event.results[i][0].transcript;
                if (event.results[i].isFinal) {
                    finalText += transcript;
                } else {
                    interimText += transcript;
                }
            }
            if (finalText) {
                options.onResult(finalText, true);
            } else if (interimText) {
                options.onResult(interimText, false);
            }
        };

        recognition.onerror = (event) => {
            const errorMap: Record<string, string> = {
                'not-allowed': '麦克风权限被拒绝，请在浏览器设置中允许麦克风访问',
                'no-speech': '未检测到语音，请重试',
                'audio-capture': '未找到麦克风设备',
                'network': '语音识别服务网络错误',
                'aborted': '语音识别被取消',
            };
            const msg = errorMap[event.error] ?? `语音识别错误: ${event.error}`;
            setErrorMessage(msg);
            setState('error');
            options.onError?.(msg);
            // 3 秒后自动恢复到 idle
            setTimeout(() => {
                setState('idle');
                setErrorMessage(null);
            }, 3000);
        };

        recognition.onend = () => {
            // 清除超时定时器
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
                timeoutRef.current = null;
            }
            if (state !== 'error') {
                setState('idle');
            }
        };

        recognitionRef.current = recognition;

        // 超时自动停止
        const maxDuration = (options.maxDuration ?? 30) * 1000;
        timeoutRef.current = window.setTimeout(() => {
            recognition.stop();
        }, maxDuration);

        recognition.start();
    }, [isSupported, state, options]);

    const stop = useCallback(() => {
        recognitionRef.current?.stop();
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
            timeoutRef.current = null;
        }
    }, []);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            recognitionRef.current?.abort();
            if (timeoutRef.current) clearTimeout(timeoutRef.current);
        };
    }, []);

    return { start, stop, state, isSupported, errorMessage };
}
```

### TypeScript 类型声明

需要为 `SpeechRecognition` 添加全局类型声明（浏览器 API 尚未在所有 TS 版本中内置）：

```typescript
// src/types/speech-recognition.d.ts
interface SpeechRecognitionEvent extends Event {
    resultIndex: number;
    results: SpeechRecognitionResultList;
}

interface SpeechRecognitionErrorEvent extends Event {
    error: string;
    message: string;
}

interface SpeechRecognition extends EventTarget {
    lang: string;
    continuous: boolean;
    interimResults: boolean;
    maxAlternatives: number;
    start(): void;
    stop(): void;
    abort(): void;
    onstart: ((event: Event) => void) | null;
    onresult: ((event: SpeechRecognitionEvent) => void) | null;
    onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
    onend: ((event: Event) => void) | null;
}

declare var SpeechRecognition: {
    new(): SpeechRecognition;
};

interface Window {
    SpeechRecognition: typeof SpeechRecognition;
    webkitSpeechRecognition: typeof SpeechRecognition;
}
```

## 完成标准

- [ ] Hook 可在 Chrome/Edge 中正常启停语音识别
- [ ] 中文语音识别返回正确的中间和最终结果
- [ ] 浏览器不支持时 `isSupported = false`
- [ ] 麦克风权限被拒时正确报错
- [ ] 30 秒超时自动停止
- [ ] 组件卸载时正确清理资源
- [ ] TypeScript 类型声明完整
