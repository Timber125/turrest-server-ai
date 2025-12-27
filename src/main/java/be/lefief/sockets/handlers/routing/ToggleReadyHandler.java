package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ToggleReadyCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class ToggleReadyHandler extends CommandTopicHandler<ToggleReadyCommand> {
    public ToggleReadyHandler() {
        super(ToggleReadyCommand.SUBJECT.name(), ToggleReadyCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<ToggleReadyCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new ToggleReadyCommand(command.getData()),
                clientSession.getClientID(),
                clientSession.getClientName()
        );
    }
}
