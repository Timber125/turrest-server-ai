package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.PlaceTowerCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class PlaceTowerListener extends CommandHandler<PlaceTowerCommand> {

    private final Turrest01GameHandler gameHandler;

    public PlaceTowerListener(Turrest01GameHandler gameHandler, PlaceTowerHandler placeTowerHandler) {
        super(placeTowerHandler);
        this.gameHandler = gameHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<PlaceTowerCommand> command, ClientSession clientSession) {
        gameHandler.handlePlaceTower(command, clientSession);
    }
}
