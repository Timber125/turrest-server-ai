package be.lefief.sockets.handlers.lobby;

import be.lefief.service.lobby.LobbyService;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.LobbyCreateHandler;
import org.springframework.stereotype.Component;

@Component
public class CreateLobbyListener extends CommandHandler<CreateLobbyCommand> {

    private final LobbyHandler lobbyHandler;

    public CreateLobbyListener(LobbyHandler lobbyHandler, LobbyCreateHandler lobbyCreateHandler){
        super(lobbyCreateHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<CreateLobbyCommand> command, SocketHandler socketHandler) {
        lobbyHandler.handleCreateLobby(command, socketHandler);
    }

}
