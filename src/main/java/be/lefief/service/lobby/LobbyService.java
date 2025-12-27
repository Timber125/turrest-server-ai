package be.lefief.service.lobby;

import be.lefief.lobby.Lobby;
import be.lefief.lobby.SocketConnectionAcceptor;
import be.lefief.sockets.ClientSession;
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
    private final Map<String, ClientSession> identifiedClients;
    private final List<ClientSession> unidentifiedClients;
    private final Map<UUID, Lobby> lobbyHosts;

    public LobbyService() {
        this.lobbyHosts = new HashMap<>();
        this.identifiedClients = new HashMap<>();
        this.unidentifiedClients = new ArrayList<>();
    }

    public Lobby getLobby(UUID lobbyID) {
        return lobbyHosts.get(lobbyID);
    }

    private String getSessionKey(UUID userId, String tabId) {
        return userId + ":" + (tabId != null ? tabId : "default");
    }

    public boolean createLobby(SecuredClientToServerCommand<CreateLobbyCommand> command) {
        UUID hostID = command.getClientId();
        String hostName = command.getClientName();
        Lobby proposedLobby = new Lobby(hostID, hostName, command.getCommand().getSize(), command.getCommand().isHidden(),
                command.getCommand().getGame());
        // Remove host from other lobbies
        lobbyHosts.values().forEach(lobby -> lobby.removeClient(hostID));
        lobbyHosts.put(hostID, proposedLobby);
        return true;
    }

    public Lobby joinLobby(SecuredClientToServerCommand<JoinLobbyCommand> command) {
        if (lobbyExists(command.getCommand().getLobbyId())) {
            if (!clientIsPartOfLobby(command.getCommand().getLobbyId(), command.getClientId())) {
                lobbyHosts.get(command.getCommand().getLobbyId()).addClient(command.getClientId(), command.getClientName());
                return lobbyHosts.get(command.getCommand().getLobbyId());
            }
        }
        return null;
    }

    private boolean clientIsPartOfLobby(UUID hostId, UUID clientId) {
        Lobby lobby = lobbyHosts.get(hostId);
        if (lobby == null)
            return false;
        return lobby.getPlayerIds().contains(clientId);
    }

    private boolean lobbyExists(UUID hostId) {
        return lobbyHosts.containsKey(hostId);
    }

    private void removeUserFromAllLobbies(UUID userId) {
        lobbyHosts.values().forEach(lobby -> lobby.removeClient(userId));
    }

    private void removeLobbyHostForUser(UUID userId) {
        if (lobbyHosts.containsKey(userId)) {
            lobbyHosts.get(userId).removeClient(userId);
        }
    }

    public void handleLogin(UUID userId, ClientSession clientSession) {
        identifiedClients.put(getSessionKey(userId, clientSession.getTabId()), clientSession);
        unidentifiedClients.remove(clientSession);
        emitGlobalMessage(CommandFactory.TIMED_SERVER_MESSAGE(LocalDateTime.now(),
                clientSession.getClientName() + " Logged in."));
    }

    private Runnable onClose(ClientSession clientSession) {
        return () -> {
            unidentifiedClients.remove(clientSession);
            UUID clientID = clientSession.getClientID();
            if (clientID != null) {
                ClientSession removed = identifiedClients.remove(getSessionKey(clientID, clientSession.getTabId()));
                removeLobbyHostForUser(clientID);
                removeUserFromAllLobbies(clientID);
                if (removed != null) {
                    emitGlobalMessage(CommandFactory.TIMED_SERVER_MESSAGE(LocalDateTime.now(),
                            removed.getUserIdentifiedClientName() + " Disconnected."));
                }
            }
        };
    }

    public void emitGlobalMessage(ServerToClientCommand serverToClientCommand) {
        identifiedClients.values().forEach(client -> client.sendCommand(serverToClientCommand));
    }

    public List<Lobby> getLobbies() {
        return lobbyHosts.values().stream()
                .filter(lobby -> !lobby.isStarted())
                .collect(java.util.stream.Collectors.toList());
    }

    public void removeLobby(UUID hostId) {
        lobbyHosts.remove(hostId);
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
        if (lobby != null) {
            lobby.getPlayerIds().forEach(playerId -> {
                identifiedClients.values().stream()
                        .filter(session -> playerId.equals(session.getClientID()))
                        .forEach(session -> session.sendMessage(message));
            });
        }
    }

    public void emitLobbyCommand(UUID lobbyId, ServerToClientCommand serverToClientCommand) {
        Lobby lobby = lobbyHosts.get(lobbyId);
        if (lobby != null) {
            lobby.getPlayerIds().forEach(playerId -> {
                identifiedClients.values().stream()
                        .filter(session -> playerId.equals(session.getClientID()))
                        .forEach(session -> session.sendCommand(serverToClientCommand));
            });
        }
    }

    public List<ClientSession> getLobbyPlayerSessions(UUID lobbyId) {
        Lobby lobby = lobbyHosts.get(lobbyId);
        if (lobby != null) {
            List<ClientSession> sessions = new ArrayList<>();
            // Iterate in lobby player order to preserve player number assignment
            for (UUID playerId : lobby.getPlayerIds()) {
                // Find exactly one session per player (first matching session)
                ClientSession session = identifiedClients.values().stream()
                        .filter(s -> playerId.equals(s.getClientID()))
                        .findFirst()
                        .orElse(null);
                if (session != null) {
                    sessions.add(session);
                }
            }
            return sessions;
        }
        return new ArrayList<>();
    }

    public Lobby findLobbyByPlayer(UUID playerId) {
        return lobbyHosts.values().stream()
                .filter(lobby -> lobby.getPlayerIds().contains(playerId))
                .findFirst()
                .orElse(null);
    }

    public ClientSession getClientSession(UUID playerId) {
        return identifiedClients.values().stream()
                .filter(session -> playerId.equals(session.getClientID()))
                .findFirst()
                .orElse(null);
    }
}
