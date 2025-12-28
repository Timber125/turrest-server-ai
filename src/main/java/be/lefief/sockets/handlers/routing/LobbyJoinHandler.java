package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class LobbyJoinHandler extends CommandTopicHandler<JoinLobbyCommand> {
    public LobbyJoinHandler() {
        super(JoinLobbyCommand.SUBJECT.name(), JoinLobbyCommand.TOPIC);
    }
    @Override
    public SecuredClientToServerCommand<JoinLobbyCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new JoinLobbyCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
