package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.sockets.commands.client.emission.RefreshLobbiesCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.LobbyListAllHandler;
import org.springframework.stereotype.Component;

@Component
public class RefreshLobbiesListener extends CommandHandler<RefreshLobbiesCommand> {

    private final LobbyHandler lobbyHandler;
    public RefreshLobbiesListener(LobbyHandler lobbyHandler, LobbyListAllHandler lobbyListAllHandler){
        super(lobbyListAllHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<RefreshLobbiesCommand> command, SocketHandler socketHandler) {
        lobbyHandler.handleGetAllLobbies(command, socketHandler);
    }

}
