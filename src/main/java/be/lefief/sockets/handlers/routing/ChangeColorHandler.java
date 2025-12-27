package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ChangeColorCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class ChangeColorHandler extends CommandTopicHandler<ChangeColorCommand> {
    public ChangeColorHandler() {
        super(ChangeColorCommand.SUBJECT.name(), ChangeColorCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<ChangeColorCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new ChangeColorCommand(command.getData()),
                clientSession.getClientID(),
                clientSession.getClientName()
        );
    }
}
