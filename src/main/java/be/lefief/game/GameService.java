package be.lefief.game;

import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.service.lobby.LobbyService;
import be.lefief.sockets.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final Logger LOG = LoggerFactory.getLogger(GameService.class);
    private final Map<UUID, Game<?>> games;
    private final Map<String, UUID> playerActiveGame;
    private final LobbyService lobbyService;

    public GameService(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
        games = new ConcurrentHashMap<>();
        playerActiveGame = new ConcurrentHashMap<>();
    }

    public Game<?> startGame(String gameType, List<ClientSession> lobbyPlayers, UUID lobbyHostId, Map<UUID, Integer> playerColorMap) {
        LOG.info("Starting game of type '{}' with {} players", gameType, lobbyPlayers.size());

        Game<?> game;
        if ("TURREST-mode1".equals(gameType)) {
            game = new TurrestGameMode01(lobbyPlayers, lobbyHostId, playerColorMap);
        } else {
            LOG.warn("Unknown game type '{}', defaulting to TURREST-mode1", gameType);
            game = new TurrestGameMode01(lobbyPlayers, lobbyHostId, playerColorMap);
        }

        games.put(game.getGameID(), game);

        for (ClientSession session : lobbyPlayers) {
            if (session.getClientID() != null) {
                String sessionKey = session.getClientID() + ":" + session.getTabId();
                playerActiveGame.put(sessionKey, game.getGameID());
            }
        }

        game.setOnGameEnd(() -> cleanupGame(game.getGameID()));
        game.start();

        LOG.info("Game {} started successfully", game.getGameID());
        return game;
    }

    public Game<?> getGame(UUID gameId) {
        return games.get(gameId);
    }

    /**
     * Gets the game a player is currently in by their session key.
     */
    public Game<?> getGameBySessionKey(String sessionKey) {
        UUID gameId = playerActiveGame.get(sessionKey);
        if (gameId != null) {
            return games.get(gameId);
        }
        return null;
    }

    public boolean isPlayerInGame(String sessionKey) {
        return playerActiveGame.containsKey(sessionKey);
    }

    public void handlePlayerDisconnect(String sessionKey, UUID userId) {
        UUID gameId = playerActiveGame.get(sessionKey);
        if (gameId != null) {
            Game<?> game = games.get(gameId);
            if (game != null) {
                game.handlePlayerDisconnect(userId);
            }
        }
    }

    public void reconnectPlayer(String sessionKey, ClientSession newSession) {
        UUID gameId = playerActiveGame.get(sessionKey);
        if (gameId != null) {
            Game<?> game = games.get(gameId);
            if (game != null) {
                LOG.info(" reconnecting session {} to game {}", sessionKey, gameId);
                game.reconnectPlayer(newSession.getClientID(), newSession);
            } else {
                LOG.warn("Session {} tried to reconnect to game {} but game instance not found", sessionKey, gameId);
                playerActiveGame.remove(sessionKey); // Cleanup orphan
            }
        }
    }

    public void unregisterPlayer(String sessionKey) {
        playerActiveGame.remove(sessionKey);
    }

    public void cleanupGame(UUID gameId) {
        Game<?> game = games.remove(gameId);
        if (game != null) {
            if (game.getLobbyHostId() != null) {
                lobbyService.removeLobby(game.getLobbyHostId());
            }
            // Remove all players associated with this game
            playerActiveGame.entrySet().removeIf(entry -> entry.getValue().equals(gameId));
            LOG.info("Game {} (from lobby {}) cleaned up", gameId, game.getLobbyHostId());
        }
    }
}
