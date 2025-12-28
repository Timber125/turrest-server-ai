package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import be.lefief.sockets.commands.client.emission.LeaveGameCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class GameLeaveHandler extends CommandTopicHandler<LeaveGameCommand> {
    public GameLeaveHandler() {
        super(ServerSocketSubject.GAME.name(), "LEAVE_GAME");
    }

    @Override
    public SecuredClientToServerCommand<LeaveGameCommand> identify(ClientToServerCommand command,
            ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new LeaveGameCommand(command),
                clientSession.getUserId(),
                clientSession.getUserName());
    }
}
