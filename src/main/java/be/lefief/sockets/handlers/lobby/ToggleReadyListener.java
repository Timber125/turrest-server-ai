package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ToggleReadyCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.ToggleReadyHandler;
import org.springframework.stereotype.Component;

@Component
public class ToggleReadyListener extends CommandHandler<ToggleReadyCommand> {

    private final LobbyHandler lobbyHandler;

    public ToggleReadyListener(LobbyHandler lobbyHandler, ToggleReadyHandler toggleReadyHandler) {
        super(toggleReadyHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<ToggleReadyCommand> command, ClientSession clientSession) {
        lobbyHandler.handleToggleReady(command, clientSession);
    }
}
