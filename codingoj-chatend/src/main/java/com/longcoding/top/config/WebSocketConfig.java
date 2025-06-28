package com.longcoding.top.config;

import com.longcoding.top.controller.websocket.InterViewWebSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InterViewWebSocket interViewWebSocket;

    public WebSocketConfig(InterViewWebSocket interViewWebSocket) {
        this.interViewWebSocket = interViewWebSocket;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interViewWebSocket, "/interview")
                .setAllowedOrigins("*");
    }
}
