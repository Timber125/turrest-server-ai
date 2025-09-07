package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ConnectionCommand;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class LobbyCreateHandler extends CommandTopicHandler<CreateLobbyCommand> {
    public LobbyCreateHandler() {
        super(CreateLobbyCommand.SUBJECT.name(), CreateLobbyCommand.TOPIC);
    }
    @Override
    public SecuredClientToServerCommand<CreateLobbyCommand> identify(ClientToServerCommand command, SocketHandler socketHandler) {
        return new SecuredClientToServerCommand<>(
                new CreateLobbyCommand(command.getData()),
                socketHandler.getClientID(),
                socketHandler.getClientName()
        );
    }
}
