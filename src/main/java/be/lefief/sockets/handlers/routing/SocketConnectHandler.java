package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ConnectionCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class SocketConnectHandler extends CommandTopicHandler<ConnectionCommand> {
    public SocketConnectHandler() {
        super(ConnectionCommand.SUBJECT.name(), ConnectionCommand.TOPIC);
    }
    @Override
    public SecuredClientToServerCommand<ConnectionCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new ConnectionCommand(command.getData()),
                null,
                null
        );
    }
}
