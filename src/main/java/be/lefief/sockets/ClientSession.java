package be.lefief.sockets;

import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.commands.ServerToClientCommand;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Abstraction for client connections - supports both TCP sockets and
 * WebSockets.
 */
public interface ClientSession {

    /**
     * Send a command to the client.
     */
    void sendCommand(ServerToClientCommand command);

    /**
     * Send a raw message string to the client.
     */
    void sendMessage(String message);

    /**
     * Get the client's UUID (null if not authenticated).
     */
    UUID getClientID();

    /**
     * Get the client's display name.
     */
    String getClientName();

    /**
     * Get formatted name with ID prefix.
     */
    String getUserIdentifiedClientName();

    /**
     * Authenticate this session with a user.
     */
    void authenticate(UserProfileService userProfileService, UUID clientId);

    /**
     * Get the user data for authenticated sessions.
     */
    UserData getUserData();

    /**
     * Close this session.
     */
    void close();

    /**
     * Set handler to run when session closes.
     */
    void setOnClose(Runnable onClose);

    /**
     * Set handler for incoming messages.
     */
    void setOnMessage(Consumer<String> onMessage);

    /**
     * Get a description of the remote address (for logging).
     */
    String getRemoteAddress();

    String getTabId();

    void setTabId(String tabId);
}
