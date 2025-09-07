package be.lefief.util;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClientListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClientListener.class);
    private ServerSocketSubject serverSocketSubject;
    public ClientListener(ServerSocketSubject subject){
        this.serverSocketSubject = subject;
    }
    public abstract void accept(SecuredClientToServerCommand securedClientToServerCommand, SocketHandler socketHandler);
    public ServerSocketSubject getSubject(){
        return serverSocketSubject;
    }

    protected void warnUnknownTopic(ClientToServerCommand command){
        LOG.warn("Unknown topic {} for subject {}", command.getTopic(), command.getSubject());
    }

}
