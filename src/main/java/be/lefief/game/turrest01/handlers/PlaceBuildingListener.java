package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.PlaceBuildingCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class PlaceBuildingListener extends CommandHandler<PlaceBuildingCommand> {

    private final Turrest01GameHandler gameHandler;

    public PlaceBuildingListener(Turrest01GameHandler gameHandler, PlaceBuildingHandler placeBuildingHandler) {
        super(placeBuildingHandler);
        this.gameHandler = gameHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<PlaceBuildingCommand> command, ClientSession clientSession) {
        gameHandler.handlePlaceBuilding(command, clientSession);
    }
}
