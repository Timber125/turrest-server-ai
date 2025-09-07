package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.StartLobbyGameCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class StartLobbyGameHandler extends CommandTopicHandler<StartLobbyGameCommand> {

    public StartLobbyGameHandler(String subject, String topic) {
        super(subject, topic);
    }

    @Override
    public SecuredClientToServerCommand<StartLobbyGameCommand> identify(ClientToServerCommand command, SocketHandler socketHandler) {
        return new SecuredClientToServerCommand<>(
                new StartLobbyGameCommand(command.getData()),
                socketHandler.getClientID(),
                socketHandler.getClientName()
        );
    }
}
