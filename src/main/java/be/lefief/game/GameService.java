package be.lefief.game;

import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.service.lobby.LobbyService;
import be.lefief.service.turrest02.PersistentStatsService;
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
    // Simplified: userId -> gameId (one session per user)
    private final Map<UUID, UUID> playerActiveGame;
    private final LobbyService lobbyService;
    private final PersistentStatsService persistentStatsService;

    public GameService(LobbyService lobbyService, PersistentStatsService persistentStatsService) {
        this.lobbyService = lobbyService;
        this.persistentStatsService = persistentStatsService;
        games = new ConcurrentHashMap<>();
        playerActiveGame = new ConcurrentHashMap<>();
    }

    public Game<?> startGame(String gameType, List<ClientSession> lobbyPlayers, UUID lobbyHostId, Map<UUID, Integer> playerColorMap) {
        LOG.info("Starting game of type '{}' with {} players", gameType, lobbyPlayers.size());

        Game<?> game;
        if ("TURREST-mode1".equals(gameType)) {
            game = new TurrestGameMode01(lobbyPlayers, lobbyHostId, playerColorMap);
        } else if ("TURREST-mode2".equals(gameType)) {
            game = new TurrestGameMode02(lobbyPlayers, lobbyHostId, playerColorMap, persistentStatsService);
        } else {
            LOG.warn("Unknown game type '{}', defaulting to TURREST-mode1", gameType);
            game = new TurrestGameMode01(lobbyPlayers, lobbyHostId, playerColorMap);
        }

        games.put(game.getGameID(), game);

        for (ClientSession session : lobbyPlayers) {
            if (session.getUserId() != null) {
                playerActiveGame.put(session.getUserId(), game.getGameID());
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
     * Gets the game a player is currently in by their userId.
     */
    public Game<?> getGameByUserId(UUID userId) {
        UUID gameId = playerActiveGame.get(userId);
        if (gameId != null) {
            return games.get(gameId);
        }
        return null;
    }

    public boolean isPlayerInGame(UUID userId) {
        return playerActiveGame.containsKey(userId);
    }

    public void handlePlayerDisconnect(UUID userId) {
        UUID gameId = playerActiveGame.get(userId);
        if (gameId != null) {
            Game<?> game = games.get(gameId);
            if (game != null) {
                game.handlePlayerDisconnect(userId);
            }
        }
    }

    public void reconnectPlayer(UUID userId, ClientSession newSession) {
        UUID gameId = playerActiveGame.get(userId);
        if (gameId != null) {
            Game<?> game = games.get(gameId);
            if (game != null) {
                LOG.info("Reconnecting user {} to game {}", userId, gameId);
                game.reconnectPlayer(userId, newSession);
            } else {
                LOG.warn("User {} tried to reconnect to game {} but game instance not found", userId, gameId);
                playerActiveGame.remove(userId); // Cleanup orphan
            }
        }
    }

    public void unregisterPlayer(UUID userId) {
        playerActiveGame.remove(userId);
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

    /**
     * Get the number of active games.
     */
    public int getActiveGameCount() {
        return (int) games.values().stream()
                .filter(Game::isGameIsRunning)
                .count();
    }

    /**
     * Get the number of online players (in active games).
     */
    public int getOnlinePlayerCount() {
        return (int) games.values().stream()
                .filter(Game::isGameIsRunning)
                .mapToLong(Game::getActiveConnectedPlayersCount)
                .sum();
    }
}
