package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ChangeColorCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.ChangeColorHandler;
import org.springframework.stereotype.Component;

@Component
public class ChangeColorListener extends CommandHandler<ChangeColorCommand> {

    private final LobbyHandler lobbyHandler;

    public ChangeColorListener(LobbyHandler lobbyHandler, ChangeColorHandler changeColorHandler) {
        super(changeColorHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<ChangeColorCommand> command, ClientSession clientSession) {
        lobbyHandler.handleChangeColor(command, clientSession);
    }
}
