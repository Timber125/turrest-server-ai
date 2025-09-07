package be.lefief.lobby;

import be.lefief.sockets.handlers.routing.CommandRouter;

import java.net.Socket;

public interface SocketConnectionAcceptor {

    void accept(Socket socket, CommandRouter commandRouter);

}
