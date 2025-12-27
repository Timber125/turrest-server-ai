package be.lefief.sockets.handlers;

import be.lefief.config.FakeKeycloak;
import be.lefief.game.GameService;
import be.lefief.service.lobby.LobbyService;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ConnectionCommand;
import be.lefief.sockets.commands.client.reception.DisplayChatCommand;
import be.lefief.sockets.handlers.routing.SocketConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ConnectionHandler extends CommandHandler<ConnectionCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionHandler.class);
    private final FakeKeycloak fakeKeycloak;
    private final UserProfileService userProfileService;
    private final LobbyService lobbyService;
    private final GameService gameService;

    public ConnectionHandler(FakeKeycloak fakeKeycloak, UserProfileService userProfileService,
            LobbyService lobbyService, SocketConnectHandler socketConnectHandler, GameService gameService) {
        super(socketConnectHandler);
        this.userProfileService = userProfileService;
        this.fakeKeycloak = fakeKeycloak;
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        openChannel();
    }

    private UUID getUserID(SecuredClientToServerCommand<ConnectionCommand> securedClientToServerCommand) {
        return Optional.ofNullable(securedClientToServerCommand.getCommand().getData().get(ConnectionCommand.USER_ID))
                .map(Object::toString).map(UUID::fromString).orElse(null);
    }

    private String getToken(SecuredClientToServerCommand<ConnectionCommand> securedClientToServerCommand) {
        return securedClientToServerCommand.getCommand().getData().get(ConnectionCommand.ACCESS_TOKEN).toString();
    }

    @Override
    public void accept(SecuredClientToServerCommand<ConnectionCommand> command, ClientSession clientSession) {
        String token = getToken(command);
        UUID userId = getUserID(command);
        String tabId = command.getCommand().getTabID();

        boolean authenticateSuccess = fakeKeycloak.useAccessToken(token, userId);
        if (authenticateSuccess) {
            LOG.info("Authentication success for user {} tab {}", userId, tabId);
            clientSession.authenticate(userProfileService, userId);
            clientSession.setTabId(tabId);

            String sessionKey = userId + ":" + tabId;
            if (gameService.isPlayerInGame(sessionKey)) {
                LOG.info("Session {} found in active game, reconnecting...", sessionKey);
                clientSession.sendCommand(new DisplayChatCommand("Reconnecting to ongoing game..."));
                gameService.reconnectPlayer(sessionKey, clientSession);
            } else {
                clientSession.sendCommand(
                        new DisplayChatCommand("Login successful - Welcome " + clientSession.getClientName()));
                lobbyService.handleLogin(userId, clientSession);
            }
        } else {
            LOG.info("Authentication failure: {} {}", token, userId.toString());
            clientSession.sendCommand(new be.lefief.sockets.commands.client.reception.TokenInvalidResponse());
            clientSession.close();
        }
    }
}
