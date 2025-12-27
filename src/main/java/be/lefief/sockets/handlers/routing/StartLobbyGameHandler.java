package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.StartLobbyGameCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class StartLobbyGameHandler extends CommandTopicHandler<StartLobbyGameCommand> {

    public StartLobbyGameHandler() {
        super(StartLobbyGameCommand.SUBJECT.name(), StartLobbyGameCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<StartLobbyGameCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new StartLobbyGameCommand(command.getData()),
                clientSession.getClientID(),
                clientSession.getClientName()
        );
    }
}
