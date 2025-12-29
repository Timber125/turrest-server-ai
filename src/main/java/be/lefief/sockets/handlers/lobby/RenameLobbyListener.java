package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.RenameLobbyCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.RenameLobbyHandler;
import org.springframework.stereotype.Component;

@Component
public class RenameLobbyListener extends CommandHandler<RenameLobbyCommand> {

    private final LobbyHandler lobbyHandler;

    public RenameLobbyListener(LobbyHandler lobbyHandler, RenameLobbyHandler renameLobbyHandler) {
        super(renameLobbyHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<RenameLobbyCommand> command, ClientSession clientSession) {
        lobbyHandler.handleRenameLobby(command, clientSession);
    }
}
