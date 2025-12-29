package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.AddBotCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class AddBotHandler extends CommandTopicHandler<AddBotCommand> {
    public AddBotHandler() {
        super(AddBotCommand.SUBJECT.name(), AddBotCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<AddBotCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new AddBotCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
