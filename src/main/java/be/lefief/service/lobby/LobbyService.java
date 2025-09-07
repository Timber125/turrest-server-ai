package be.lefief.service.lobby;

import be.lefief.lobby.Lobby;
import be.lefief.lobby.SocketConnectionAcceptor;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.sockets.commands.factories.CommandFactory;
import be.lefief.sockets.handlers.routing.CommandRouter;
import be.lefief.util.ClientCommandConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LobbyService implements SocketConnectionAcceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LobbyService.class);
    private final Map<UUID, SocketHandler> identifiedClients;
    private final List<SocketHandler> unidentifiedClients;
    private final Map<UUID, Lobby> lobbyHosts;
    public LobbyService() {
        this.lobbyHosts = new HashMap<>();
        this.identifiedClients = new HashMap<>();
        this.unidentifiedClients = new ArrayList<>();
    }

    public Lobby getLobby(UUID lobbyID){
        return lobbyHosts.get(lobbyID);
    }

    public boolean createLobby(SecuredClientToServerCommand<CreateLobbyCommand> command) {
        UUID hostID = command.getClientId();
        Lobby proposedLobby = new Lobby(hostID, command.getCommand().getSize(), command.getCommand().isHidden(), command.getCommand().getGame());
        lobbyHosts.values().forEach(lobby -> lobby.getPlayers().remove(hostID));
        lobbyHosts.put(hostID, proposedLobby);
        return true;
    }

    public Lobby joinLobby(SecuredClientToServerCommand<JoinLobbyCommand> command) {
        if (lobbyExists(command.getCommand().getLobbyId())) {
            if (!clientIsPartOfLobby(command.getCommand().getLobbyId(), command.getClientId())) {
                lobbyHosts.get(command.getCommand().getLobbyId()).addClient(command.getClientId());
                return lobbyHosts.get(command.getCommand().getLobbyId());
            }
        }
        return null;
    }

    private boolean clientIsPartOfLobby(UUID hostId, UUID clientId) {
        Lobby lobby = lobbyHosts.get(hostId);
        if (lobby == null) return false;
        return lobby.getPlayers().contains(clientId);
    }

    private boolean lobbyExists(UUID hostId) {
        return lobbyHosts.containsKey(hostId);
    }

    private void removeUserFromAllLobbies(UUID userId) {
        lobbyHosts.values().forEach(lobby -> lobby.getPlayers().remove(userId));
    }

    private void removeLobbyHostForUser(UUID userId) {
        if (lobbyHosts.containsKey(userId)) {
            lobbyHosts.get(userId).removeClient(userId);
        }
    }

    public void handleLogin(UUID userId, SocketHandler socketHandler) {
        identifiedClients.put(userId, socketHandler);
        unidentifiedClients.remove(socketHandler);
        emitGlobalMessage(CommandFactory.TIMED_SERVER_MESSAGE(LocalDateTime.now(), socketHandler.getClientName() + " Logged in."));
    }

    private Runnable onClose(SocketHandler socketHandler) {
        return () -> {
            unidentifiedClients.remove(socketHandler);
            UUID clientID = socketHandler.getClientID();
            if (clientID == null) {
                unidentifiedClients.remove(socketHandler);
            } else {
                SocketHandler removed = identifiedClients.remove(clientID);
                removeLobbyHostForUser(clientID);
                removeUserFromAllLobbies(clientID);
                emitGlobalMessage(CommandFactory.TIMED_SERVER_MESSAGE(LocalDateTime.now(), removed.getUserIdentifiedClientName() + " Disconnected."));
            }
        };
    }

    public void emitGlobalMessage(ServerToClientCommand serverToClientCommand) {
        identifiedClients.values().forEach(client -> client.sendCommand(serverToClientCommand));
    }

    public List<Lobby> getLobbies() {
        return new ArrayList<>(lobbyHosts.values());
    }

    @Override
    public void accept(Socket socket, CommandRouter commandRouter) {
        LOG.info("Client connected to lobby: {}", socket.getInetAddress());
        SocketHandler socketHandler = new SocketHandler(socket);
        socketHandler.setOnClose(onClose(socketHandler));
        socketHandler.setOnMessage(ClientCommandConsumer.createCommandConsumer(socketHandler, commandRouter));
        unidentifiedClients.add(socketHandler);
        new Thread(socketHandler::run).start();
    }

    public void emitLobbyMessage(UUID lobbyId, String message) {
        Lobby lobby = lobbyHosts.get(lobbyId);
        if(lobby != null){
            lobby.getPlayers().forEach(playerId -> identifiedClients.get(playerId).sendMessage(message));
        }
    }

    public void emitLobbyCommand(UUID lobbyId, ServerToClientCommand serverToClientCommand) {
        Lobby lobby = lobbyHosts.get(lobbyId);
        if(lobby != null){
            lobby.getPlayers().forEach(playerId -> identifiedClients.get(playerId).sendCommand(serverToClientCommand));
        }
    }
}
