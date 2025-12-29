package be.lefief.game.ai;

import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.commands.ServerToClientCommand;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Mock ClientSession for bot players.
 * Discards incoming server commands (bots don't need UI updates).
 */
public class BotSession implements ClientSession {

    private final UUID botId;
    private final String botName;
    private Runnable onClose;

    public BotSession(UUID botId, String botName) {
        this.botId = botId;
        this.botName = botName;
    }

    public BotSession(String botName) {
        this(UUID.randomUUID(), botName);
    }

    @Override
    public void sendMessage(String message) {
        // Bots don't process raw messages
    }

    @Override
    public void sendCommand(ServerToClientCommand serverToClientCommand) {
        // Bots don't need UI updates - they access game state directly
    }

    @Override
    public void close() {
        if (onClose != null) {
            onClose.run();
        }
    }

    @Override
    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

    @Override
    public UUID getUserId() {
        return botId;
    }

    @Override
    public void setOnMessage(Consumer<String> stringConsumer) {
        // Bots don't receive messages
    }

    @Override
    public String getUserName() {
        return botName;
    }

    @Override
    public String getUserIdentifiedClientName() {
        return botName + " (Bot)";
    }

    @Override
    public void authenticate(UserProfileService userProfileService, UUID userId) {
        // Bots are pre-authenticated
    }

    @Override
    public UserData getUserData() {
        return new UserData(botId, botName, LocalDateTime.now());
    }

    @Override
    public String getRemoteAddress() {
        return "bot://localhost";
    }
}
