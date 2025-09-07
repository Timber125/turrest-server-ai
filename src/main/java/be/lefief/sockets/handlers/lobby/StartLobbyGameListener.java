package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.sockets.commands.client.emission.StartLobbyGameCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.LobbyCreateHandler;
import be.lefief.sockets.handlers.routing.StartLobbyGameHandler;
import org.springframework.stereotype.Component;

@Component
public class StartLobbyGameListener extends CommandHandler<StartLobbyGameCommand> {

    private final LobbyHandler lobbyHandler;

    public StartLobbyGameListener(LobbyHandler lobbyHandler, StartLobbyGameHandler startLobbyGameHandler){
        super(startLobbyGameHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<StartLobbyGameCommand> command, SocketHandler socketHandler) {
        lobbyHandler.handleStartLobbyGame(command, socketHandler);
    }

}
