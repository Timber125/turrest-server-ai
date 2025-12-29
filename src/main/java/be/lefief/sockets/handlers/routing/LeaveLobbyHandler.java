package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.LeaveLobbyCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class LeaveLobbyHandler extends CommandTopicHandler<LeaveLobbyCommand> {
    public LeaveLobbyHandler() {
        super(LeaveLobbyCommand.SUBJECT.name(), LeaveLobbyCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<LeaveLobbyCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new LeaveLobbyCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
