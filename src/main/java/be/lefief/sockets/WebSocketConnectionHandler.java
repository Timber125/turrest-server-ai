package be.lefief.sockets;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.handlers.routing.CommandRouter;
import be.lefief.util.CommandSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles WebSocket connections and bridges them to the existing command protocol.
 */
@Component
public class WebSocketConnectionHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketConnectionHandler.class);

    private final CommandRouter commandRouter;
    private final Map<String, WebSocketClientSession> sessions = new ConcurrentHashMap<>();

    public WebSocketConnectionHandler(CommandRouter commandRouter) {
        this.commandRouter = commandRouter;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LOG.info("WebSocket client connected: {}", session.getRemoteAddress());

        WebSocketClientSession clientSession = new WebSocketClientSession(session);
        sessions.put(session.getId(), clientSession);

        // Set up message handler (similar to ClientCommandConsumer)
        clientSession.setOnMessage(message -> {
            LOG.info("WebSocket client {} @ {} sent: {}",
                    Optional.ofNullable(clientSession.getClientID()).map(UUID::toString).orElse("<unauthenticated>"),
                    clientSession.getRemoteAddress(),
                    message);

            ClientToServerCommand command = CommandSerializer.deserializeClientToServerCommand(message);
            if (command != null) {
                commandRouter.handle(command, clientSession);
            }
        });

        // Set up close handler
        clientSession.setOnClose(() -> {
            LOG.info("WebSocket client {} disconnected", clientSession.getClientID());
            sessions.remove(session.getId());
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketClientSession clientSession = sessions.get(session.getId());
        if (clientSession != null) {
            clientSession.handleMessage(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOG.info("WebSocket connection closed: {} - {}", session.getId(), status);
        WebSocketClientSession clientSession = sessions.remove(session.getId());
        if (clientSession != null) {
            clientSession.handleClose();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        LOG.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        WebSocketClientSession clientSession = sessions.get(session.getId());
        if (clientSession != null) {
            clientSession.close();
        }
    }

    /**
     * Get all active WebSocket sessions.
     */
    public Map<String, WebSocketClientSession> getSessions() {
        return sessions;
    }
}
