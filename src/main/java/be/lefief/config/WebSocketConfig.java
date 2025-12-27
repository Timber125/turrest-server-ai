package be.lefief.config;

import be.lefief.sockets.WebSocketConnectionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketConnectionHandler webSocketConnectionHandler;

    public WebSocketConfig(WebSocketConnectionHandler webSocketConnectionHandler) {
        this.webSocketConnectionHandler = webSocketConnectionHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketConnectionHandler, "/ws/lobby")
                .setAllowedOrigins("*");
    }
}
