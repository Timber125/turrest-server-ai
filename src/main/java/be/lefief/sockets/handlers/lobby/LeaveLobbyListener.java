package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.LeaveLobbyCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.LeaveLobbyHandler;
import org.springframework.stereotype.Component;

@Component
public class LeaveLobbyListener extends CommandHandler<LeaveLobbyCommand> {

    private final LobbyHandler lobbyHandler;

    public LeaveLobbyListener(LobbyHandler lobbyHandler, LeaveLobbyHandler leaveLobbyHandler) {
        super(leaveLobbyHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<LeaveLobbyCommand> command, ClientSession clientSession) {
        lobbyHandler.handleLeaveLobby(command, clientSession);
    }
}
