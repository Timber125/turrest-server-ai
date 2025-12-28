package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.PlaceTowerCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class PlaceTowerHandler extends CommandTopicHandler<PlaceTowerCommand> {

    public PlaceTowerHandler() {
        super(PlaceTowerCommand.SUBJECT.name(), PlaceTowerCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<PlaceTowerCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new PlaceTowerCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
