package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.sockets.commands.client.emission.RefreshLobbiesCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class LobbyListAllHandler extends CommandTopicHandler<RefreshLobbiesCommand> {
    public LobbyListAllHandler() {
        super(RefreshLobbiesCommand.SUBJECT.name(), RefreshLobbiesCommand.TOPIC);
    }
    @Override
    public SecuredClientToServerCommand<RefreshLobbiesCommand> identify(ClientToServerCommand command, SocketHandler socketHandler) {
        return new SecuredClientToServerCommand<>(
                new RefreshLobbiesCommand(command.getData()),
                socketHandler.getClientID(),
                socketHandler.getClientName()
        );
    }
}
