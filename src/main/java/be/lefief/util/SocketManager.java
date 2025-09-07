package be.lefief.util;

import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.handlers.routing.CommandRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SocketManager {
    private static final Logger LOG = LoggerFactory.getLogger(SocketManager.class);
    private final CommandRouter commandRouter;
    public SocketManager(
            CommandRouter commandRouter
    ){
        this.commandRouter = commandRouter;
    }
    public void acceptClientData(SocketHandler socketHandler, ClientToServerCommand command){
        LOG.info("Incoming client command: " + command.getSubject() + " " + command.getTopic());
        commandRouter.handle(command, socketHandler);
    }

}
