package be.lefief.sockets.handlers.lobby;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.AddBotCommand;
import be.lefief.sockets.handlers.CommandHandler;
import be.lefief.sockets.handlers.LobbyHandler;
import be.lefief.sockets.handlers.routing.AddBotHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AddBotListener extends CommandHandler<AddBotCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(AddBotListener.class);

    private final LobbyHandler lobbyHandler;

    public AddBotListener(LobbyHandler lobbyHandler, AddBotHandler addBotHandler) {
        super(addBotHandler);
        this.lobbyHandler = lobbyHandler;
        openChannel();
        LOG.info("AddBotListener initialized and channel opened");
    }

    @Override
    public void accept(SecuredClientToServerCommand<AddBotCommand> command, ClientSession clientSession) {
        LOG.info("AddBotListener.accept() called - forwarding to LobbyHandler");
        lobbyHandler.handleAddBot(command, clientSession);
    }
}
