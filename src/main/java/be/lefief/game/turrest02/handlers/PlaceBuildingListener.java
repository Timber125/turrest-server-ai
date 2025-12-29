package be.lefief.game.turrest02.handlers;

import be.lefief.game.turrest02.commands.PlaceBuildingCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class PlaceBuildingListener extends CommandHandler<PlaceBuildingCommand> {

    private final Turrest02GameHandler gameHandler;

    public PlaceBuildingListener(Turrest02GameHandler gameHandler, PlaceBuildingHandler placeBuildingHandler) {
        super(placeBuildingHandler);
        this.gameHandler = gameHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<PlaceBuildingCommand> command, ClientSession clientSession) {
        gameHandler.handlePlaceBuilding(command, clientSession);
    }
}
