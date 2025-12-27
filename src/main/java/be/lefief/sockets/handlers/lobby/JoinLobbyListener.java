package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.LobbyJoinHandler;
import org.springframework.stereotype.Component;

@Component
public class JoinLobbyListener extends CommandHandler<JoinLobbyCommand> {

    private final LobbyHandler lobbyHandler;

    public JoinLobbyListener(LobbyHandler lobbyHandler, LobbyJoinHandler lobbyJoinHandler){
        super(lobbyJoinHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<JoinLobbyCommand> command, ClientSession clientSession) {
        lobbyHandler.handleJoinLobby(command, clientSession);
    }

}
