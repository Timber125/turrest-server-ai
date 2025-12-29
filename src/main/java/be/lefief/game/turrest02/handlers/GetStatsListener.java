package be.lefief.game.turrest02.handlers;

import be.lefief.game.turrest02.commands.GetStatsCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import org.springframework.stereotype.Component;

@Component("turrest02GetStatsListener")
public class GetStatsListener extends CommandHandler<GetStatsCommand> {

    private final Turrest02GameHandler gameHandler;

    public GetStatsListener(Turrest02GameHandler gameHandler, GetStatsHandler getStatsHandler) {
        super(getStatsHandler);
        this.gameHandler = gameHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<GetStatsCommand> command, ClientSession clientSession) {
        gameHandler.handleGetStats(command, clientSession);
    }
}
