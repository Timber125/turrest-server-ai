package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.GetStatsCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class GetStatsListener extends CommandHandler<GetStatsCommand> {

    private final Turrest01GameHandler gameHandler;

    public GetStatsListener(Turrest01GameHandler gameHandler, GetStatsHandlerT01 getStatsHandlerT01) {
        super(getStatsHandlerT01);
        this.gameHandler = gameHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<GetStatsCommand> command, ClientSession clientSession) {
        gameHandler.handleGetStats(command, clientSession);
    }
}
