package com.kb.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 配置
 * <p>
 * 用于实时推送文档处理状态、系统通知等
 * 前端通过 SockJS + STOMP 订阅：
 * <pre>
 * const socket = new SockJS('/ws');
 * const client = Stomp.over(socket);
 * client.connect({}, () => {
 *   client.subscribe('/topic/notifications', (msg) => { ... });
 *   client.subscribe('/user/queue/document-status', (msg) => { ... });
 * });
 * </pre>
 * </p>
 * @author forever-king
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端订阅前缀
        registry.enableSimpleBroker("/topic", "/queue");
        // 服务端接收前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 用户私有队列前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
