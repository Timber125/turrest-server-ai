package be.lefief.lobby;

import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.handlers.routing.CommandRouter;
import be.lefief.util.ClientCommandConsumer;
import be.lefief.util.CommandSerializer;
import be.lefief.util.SocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.Socket;
import java.util.*;

@Service
public class LobbyManager implements SocketConnectionAcceptor {
    private static final Logger LOG = LoggerFactory.getLogger(LobbyManager.class);
    private final List<SocketHandler> clientsInLobby;
    public LobbyManager() {
        this.clientsInLobby = new ArrayList<>();
    }
    @Override
    public void accept(Socket socket, CommandRouter commandRouter) {
        LOG.info("Client connected to lobby: {}", socket.getInetAddress());
        SocketHandler socketHandler = new SocketHandler(socket);
        socketHandler.setOnClose(onClose(socketHandler));
        socketHandler.setOnMessage(ClientCommandConsumer.createCommandConsumer(socketHandler, commandRouter));
        clientsInLobby.add(socketHandler);
        new Thread(socketHandler::run).start();
    }

    private Runnable onClose(SocketHandler socketHandler) {
        return () -> clientsInLobby.remove(socketHandler);
    }

    public void emitGlobalMessage(ServerToClientCommand serverToClientCommand) {
        clientsInLobby.forEach(client -> client.sendCommand(serverToClientCommand));
    }
    public List<SocketHandler> getClientsInLobby(){
        return clientsInLobby;
    }

}
