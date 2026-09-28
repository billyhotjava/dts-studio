package com.example.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 示例：园林平台 Gateway 认证转换过滤器
 * 从园林平台 JWT Token 中提取用户信息，转换为 dts-copilot 所需的请求头。
 *
 * <p>使用方式：将此类添加到 rs-gateway 模块中，并在 application.yml 中配置 copilot.api-key</p>
 *
 * <p>工作流程：
 * <ol>
 *   <li>园林平台网关已完成 JWT 认证，将用户信息写入请求头</li>
 *   <li>本过滤器拦截 /copilot/** 请求</li>
 *   <li>将园林平台用户头转换为 dts-copilot 格式</li>
 *   <li>注入 copilot API Key 作为 Bearer Token</li>
 * </ol>
 * </p>
 */
@Component
public class CopilotAuthConvertFilter implements GlobalFilter, Ordered {

    @Value("${copilot.api-key:}")
    private String copilotApiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/copilot/")) {
            return chain.filter(exchange);
        }

        // 从园林平台的认证上下文中获取用户信息
        // 这些请求头由园林平台的 AuthFilter 在 JWT 验证后设置
        // 实际字段名需根据园林平台的 JWT 结构调整
        ServerHttpRequest request = exchange.getRequest();
        String userId = request.getHeaders().getFirst("X-Access-User-Id");
        String userName = request.getHeaders().getFirst("X-Access-User-Name");
        String nickName = request.getHeaders().getFirst("X-Access-Nick-Name");

        ServerHttpRequest mutatedRequest = request.mutate()
                // 替换为 copilot API Key 认证
                .header("Authorization", "Bearer " + copilotApiKey)
                // 传递用户身份信息
                .header("X-DTS-User-Id", userId != null ? userId : "anonymous")
                .header("X-DTS-User-Name", userName != null ? userName : "")
                .header("X-DTS-Display-Name", nickName != null ? nickName : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 在园林平台 AuthFilter 之后执行，确保 JWT 已解析
        return -10;
    }
}
