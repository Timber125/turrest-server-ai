package be.lefief.sockets.handlers;

import be.lefief.game.GameService;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.LeaveGameCommand;
import be.lefief.sockets.handlers.routing.GameLeaveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GameLeaveListener extends CommandHandler<LeaveGameCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(GameLeaveListener.class);
    private final GameService gameService;

    public GameLeaveListener(GameService gameService, GameLeaveHandler gameLeaveHandler) {
        super(gameLeaveHandler);
        this.gameService = gameService;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<LeaveGameCommand> command, ClientSession clientSession) {
        UUID userId = clientSession.getClientID();
        if (userId != null) {
            String sessionKey = userId + ":" + clientSession.getTabId();
            LOG.info("Player {} is leaving the game explicitly from session {}", userId, sessionKey);
            gameService.handlePlayerDisconnect(sessionKey, userId);
            gameService.unregisterPlayer(sessionKey);
            // After unregistering, they are back in "lobby mode" logically for the server.
            // Client should probably navigate back.
        }
    }
}
