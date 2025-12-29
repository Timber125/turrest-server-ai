package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.RemoveBotCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.RemoveBotHandler;
import org.springframework.stereotype.Component;

@Component
public class RemoveBotListener extends CommandHandler<RemoveBotCommand> {

    private final LobbyHandler lobbyHandler;

    public RemoveBotListener(LobbyHandler lobbyHandler, RemoveBotHandler removeBotHandler) {
        super(removeBotHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<RemoveBotCommand> command, ClientSession clientSession) {
        lobbyHandler.handleRemoveBot(command, clientSession);
    }
}
