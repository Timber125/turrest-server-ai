package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.PlaceBuildingCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class PlaceBuildingHandler extends CommandTopicHandler<PlaceBuildingCommand> {

    public PlaceBuildingHandler() {
        super(PlaceBuildingCommand.SUBJECT.name(), PlaceBuildingCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<PlaceBuildingCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new PlaceBuildingCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
