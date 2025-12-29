package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.RemoveBotCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class RemoveBotHandler extends CommandTopicHandler<RemoveBotCommand> {
    public RemoveBotHandler() {
        super(RemoveBotCommand.SUBJECT.name(), RemoveBotCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<RemoveBotCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new RemoveBotCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
