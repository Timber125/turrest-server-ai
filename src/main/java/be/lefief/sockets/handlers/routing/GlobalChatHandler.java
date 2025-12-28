package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.ConnectionCommand;
import be.lefief.sockets.commands.client.emission.GlobalChatCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class GlobalChatHandler extends CommandTopicHandler<GlobalChatCommand> {
    public GlobalChatHandler() {
        super(GlobalChatCommand.SUBJECT.name(), GlobalChatCommand.TOPIC);
    }
    @Override
    public SecuredClientToServerCommand<GlobalChatCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new GlobalChatCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
