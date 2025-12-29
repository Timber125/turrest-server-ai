package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.GetStatsCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class GetStatsHandlerT01 extends CommandTopicHandler<GetStatsCommand> {

    public GetStatsHandlerT01() {
        super(GetStatsCommand.SUBJECT.name(), GetStatsCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<GetStatsCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new GetStatsCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
