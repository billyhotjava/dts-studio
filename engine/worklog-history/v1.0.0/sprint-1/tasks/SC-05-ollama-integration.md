# SC-05: Ollama 容器集成与健康检查

**状态**: READY
**依赖**: SC-04

## 目标

配置 Ollama 容器，预拉取默认模型，实现 copilot-ai 对 Ollama 的健康检查和可用性探测。

## 技术设计

### Ollama 容器配置

```yaml
copilot-ollama:
  image: ollama/ollama:latest
  ports: ["11434:11434"]
  volumes:
    - ollama-data:/root/.ollama
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: all
            capabilities: [gpu]  # 可选，无 GPU 时 CPU 模式
  healthcheck:
    test: ["CMD", "curl", "-fsS", "http://localhost:11434/api/tags"]
    interval: 30s
    timeout: 10s
    retries: 3
```

### 模型初始化脚本

```bash
#!/bin/bash
# init-ollama.sh — 首次启动时拉取默认模型
ollama pull qwen2.5-coder:7b
```

### copilot-ai 健康检查集成

```java
@Component
public class OllamaHealthIndicator implements HealthIndicator {
    // 检查 Ollama /api/tags 端点
    // UP: Ollama 可达
    // DOWN: 不可达（不阻塞启动，AI 功能降级）
}
```

### 配置

```yaml
dts:
  copilot:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://copilot-ollama:11434}
      default-model: ${OLLAMA_DEFAULT_MODEL:qwen2.5-coder:7b}
      timeout-seconds: 60
```

## 影响文件

- `dts-copilot/docker-compose.yml`（修改：完善 ollama 服务配置）
- `dts-copilot/docker/ollama/init-ollama.sh`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/health/OllamaHealthIndicator.java`（新建）
- `dts-copilot-ai/src/main/resources/application.yml`（修改：添加 ollama 配置段）

## 完成标准

- [ ] Ollama 容器启动且健康检查通过
- [ ] `curl http://localhost:11434/api/tags` 返回模型列表
- [ ] copilot-ai `/actuator/health` 包含 ollama 组件状态
- [ ] Ollama 不可用时 copilot-ai 仍可启动（降级模式）
