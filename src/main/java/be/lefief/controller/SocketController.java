package be.lefief.controller;

import be.lefief.lobby.SocketConnectionAcceptor;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.handlers.routing.CommandRouter;
import be.lefief.util.ClientCommandConsumer;
import be.lefief.util.SocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class SocketController {

    private static final Logger LOG = LoggerFactory.getLogger(SocketController.class);
    private int port;
    private ServerSocket serverSocket;
    private Thread acceptConnectionThread;
    public SocketController(int port){
        this.port = port;
    }

    public void startAcceptConnections(SocketConnectionAcceptor socketConnectionAcceptor, CommandRouter commandRouter){
        if(acceptConnectionThread == null) {
            acceptConnectionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LOG.info("Start accepting connections on port {}", port);
                    try {
                        serverSocket = new ServerSocket(port);
                        while (true) {
                            Socket clientSocket = serverSocket.accept();
                            new Thread(() -> socketConnectionAcceptor.accept(clientSocket, commandRouter)).start();
                        }
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            });
            acceptConnectionThread.start();
        }
    }

    public void stopAcceptConnections(){
        if(acceptConnectionThread != null){
            LOG.info("Stopping accepting connections on port {}", port);
            acceptConnectionThread.stop();
            acceptConnectionThread.interrupt();
            acceptConnectionThread = null;
            LOG.info("Stopped accepting connections on port {}", port);
        }
    }

}
