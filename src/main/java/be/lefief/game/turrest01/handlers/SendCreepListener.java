package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.SendCreepCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class SendCreepListener extends CommandHandler<SendCreepCommand> {

    private final Turrest01GameHandler gameHandler;

    public SendCreepListener(Turrest01GameHandler gameHandler, SendCreepHandler sendCreepHandler) {
        super(sendCreepHandler);
        this.gameHandler = gameHandler;
        openChannel();
    }

    @Override
    public void accept(SecuredClientToServerCommand<SendCreepCommand> command, ClientSession clientSession) {
        gameHandler.handleSendCreep(command, clientSession);
    }
}
