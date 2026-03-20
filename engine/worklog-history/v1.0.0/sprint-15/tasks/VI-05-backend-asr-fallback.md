# VI-05: 后端 ASR 降级接口（可选）

**优先级**: P2
**状态**: READY
**依赖**: VI-03

## 目标

为不支持 Web Speech API 的浏览器（Firefox、微信）提供后端语音识别降级通道。

## 技术设计

### API 设计

```
POST /api/ai/voice/transcribe
Content-Type: multipart/form-data

Parameters:
  audio: File (WebM/OGG/WAV, max 30s, max 5MB)
  lang: string (default: zh-CN)

Response 200:
{
  "text": "识别出的文本",
  "confidence": 0.95,
  "duration": 3.2
}

Response 400: audio missing or too large
Response 503: ASR service unavailable
```

### 后端实现

在 `copilot-ai` 中新增：

```java
@RestController
@RequestMapping("/api/ai/voice")
public class VoiceTranscribeResource {

    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribe(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "lang", defaultValue = "zh-CN") String lang) {

        // 1. 验证文件大小 (< 5MB)
        // 2. 转发到配置的 ASR 服务
        // 3. 返回识别文本
    }
}
```

### ASR 服务接入

**选项 A: 阿里云语音识别（推荐，已有通义千问账号）**

```yaml
dts:
  voice:
    enabled: false
    provider: aliyun  # aliyun / iflytek / whisper
    aliyun:
      app-key: ${DTS_VOICE_ALIYUN_APPKEY:}
      access-key-id: ${DTS_VOICE_ALIYUN_AK:}
      access-key-secret: ${DTS_VOICE_ALIYUN_SK:}
```

**选项 B: 讯飞语音（中文最优）**

**选项 C: 本地 Whisper（完全私有）**
- 需要 Python 服务或 Java 调用 whisper.cpp
- 太重，不推荐 v1

### 前端降级逻辑

当 `isSupported = false` 时，useVoiceInput 切换到 MediaRecorder 模式：

```typescript
// 录音 → blob → POST to /api/ai/voice/transcribe
const mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
// ... 录音完成后上传
```

## 完成标准

- [ ] API 接口定义完成
- [ ] 至少一个 ASR 服务可用
- [ ] 前端降级录音 → 上传 → 识别链路通
- [ ] 30 秒内返回结果
- [ ] 配置关闭时 API 返回 503
