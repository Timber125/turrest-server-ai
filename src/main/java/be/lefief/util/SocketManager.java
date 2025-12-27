package be.lefief.util;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.handlers.routing.CommandRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketManager {
    private static final Logger LOG = LoggerFactory.getLogger(SocketManager.class);
    private final CommandRouter commandRouter;
    public SocketManager(
            CommandRouter commandRouter
    ){
        this.commandRouter = commandRouter;
    }
    public void acceptClientData(ClientSession clientSession, ClientToServerCommand command){
        LOG.info("Incoming client command: " + command.getSubject() + " " + command.getTopic());
        commandRouter.handle(command, clientSession);
    }

}
