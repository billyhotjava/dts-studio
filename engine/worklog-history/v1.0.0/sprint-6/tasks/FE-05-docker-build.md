# FE-05: Dockerfile 与静态资源打包

**状态**: READY
**依赖**: FE-01~04

## 目标

创建 copilot-webapp 的 Dockerfile，通过 Nginx 提供静态资源服务。

## 技术设计

### Dockerfile

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### Nginx 配置

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理（可选，也可直接前端跨域调用）
    location /api/ai/ {
        proxy_pass http://copilot-ai:8091;
    }

    location /api/ {
        proxy_pass http://copilot-analytics:8092;
    }
}
```

## 影响文件

- `dts-copilot-webapp/Dockerfile`（新建）
- `dts-copilot-webapp/nginx.conf`（新建）
- `dts-copilot-webapp/.dockerignore`（新建）

## 完成标准

- [ ] Docker 镜像构建成功
- [ ] Nginx 正确提供静态资源
- [ ] SPA 路由回退正常（刷新不 404）
- [ ] API 代理转发正常
