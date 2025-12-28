package be.lefief.sockets;

import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.reception.DisplayChatCommand;
import be.lefief.util.CommandSerializer;
import be.lefief.util.ServerClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * WebSocket implementation of ClientSession for browser connections.
 */
public class WebSocketClientSession implements ClientSession {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientSession.class);

    private final WebSocketSession session;
    private final ExecutorService threadPoolExecutor;
    private Runnable onClose;
    private Consumer<String> onMessage;
    private Supplier<UserData> userDataSupplier;
    private final TimerTask authTimeoutTask;

    public WebSocketClientSession(WebSocketSession session) {
        this.session = session;
        this.threadPoolExecutor = Executors.newFixedThreadPool(1);

        // Timeout for authentication
        this.authTimeoutTask = new TimerTask() {
            @Override
            public void run() {
                if (userDataSupplier == null && session.isOpen()) {
                    sendMessage(CommandSerializer.serialize(
                            new DisplayChatCommand("did not receive login information in time, please retry")));
                }
            }
        };
        ServerClock.TIMER.schedule(authTimeoutTask, 10000L);
    }

    @Override
    public void sendCommand(ServerToClientCommand command) {
        if (threadPoolExecutor.isShutdown()) {
            LOG.debug("Cannot send command - executor is shutdown for session");
            return;
        }
        final String cmd = CommandSerializer.serialize(command);
        threadPoolExecutor.submit(() -> sendMessage(cmd));
    }

    @Override
    public void sendMessage(String message) {
        if (session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                LOG.error("Failed to send message to WebSocket client: {}", e.getMessage());
            }
        }
    }

    @Override
    public UUID getUserId() {
        return (userDataSupplier == null) ? null : userDataSupplier.get().getId();
    }

    @Override
    public String getUserName() {
        return getUserData().getName();
    }

    @Override
    public String getUserIdentifiedClientName() {
        return String.format("%s(%s)", getUserName(), "#" + getUserId().toString().substring(0, 4));
    }

    @Override
    public void authenticate(UserProfileService userProfileService, UUID userId) {
        if (authTimeoutTask != null) {
            authTimeoutTask.cancel();
        }
        userDataSupplier = () -> userProfileService.findByID(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public UserData getUserData() {
        return (userDataSupplier == null) ? null : userDataSupplier.get();
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException e) {
            LOG.error("Failed to close WebSocket session: {}", e.getMessage());
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    @Override
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public void setOnMessage(Consumer<String> onMessage) {
        this.onMessage = onMessage;
    }

    @Override
    public String getRemoteAddress() {
        return session.getRemoteAddress() != null ? session.getRemoteAddress().toString() : "unknown";
    }

    /**
     * Handle incoming message from the WebSocket.
     */
    public void handleMessage(String message) {
        if (onMessage != null) {
            onMessage.accept(message);
        }
    }

    /**
     * Handle session close event.
     */
    public void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
        threadPoolExecutor.shutdown();
    }

    public WebSocketSession getSession() {
        return session;
    }
}
