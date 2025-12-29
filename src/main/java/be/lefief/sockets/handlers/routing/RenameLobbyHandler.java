package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.RenameLobbyCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class RenameLobbyHandler extends CommandTopicHandler<RenameLobbyCommand> {
    public RenameLobbyHandler() {
        super(RenameLobbyCommand.SUBJECT.name(), RenameLobbyCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<RenameLobbyCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new RenameLobbyCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
