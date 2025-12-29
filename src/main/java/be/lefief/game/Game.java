package be.lefief.game;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.commands.ServerToClientCommand;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public abstract class Game<T extends Player> {
    private static final Logger LOG = LoggerFactory.getLogger(Game.class);
    private static final int COMMUNICATION_POOL_SIZE = 4;

    private Map<Integer, T> playerByNumber;
    private UUID gameID;
    private UUID lobbyHostId;
    private ExecutorService communicationPool;

    @Setter
    private boolean gameIsRunning = true;

    @Setter
    private Runnable onGameEnd;

    public Game(List<ClientSession> players, UUID lobbyHostId, Map<UUID, Integer> playerColorMap) {
        playerByNumber = new HashMap<>();
        gameID = UUID.randomUUID();
        this.lobbyHostId = lobbyHostId;
        this.communicationPool = Executors.newFixedThreadPool(COMMUNICATION_POOL_SIZE);
        for (int i = 0; i < players.size(); i++) {
            ClientSession session = players.get(i);
            int colorIndex = playerColorMap.getOrDefault(session.getUserId(), i);
            playerByNumber.put(i, createPlayer(session, i, gameID, colorIndex));
        }
    }

    /**
     * Factory method for creating players. Must be implemented by subclasses.
     */
    protected abstract T createPlayer(ClientSession session, int playerNumber, UUID gameId, int colorIndex);

    public abstract void start();

    public abstract void stop();

    protected abstract void resyncPlayer(T player);

    public void broadcastToAllPlayers(ServerToClientCommand command) {
        for (T player : playerByNumber.values()) {
            sendToPlayer(player, command);
        }
    }

    public void sendToPlayer(T player, ServerToClientCommand command) {
        if (player.isConnected() && player.getClientSession() != null) {
            // Send directly - ClientSession already handles threading
            try {
                player.getClientSession().sendCommand(command);
            } catch (Exception e) {
                LOG.error("Failed to send command to player {}", player.getPlayerNumber(), e);
            }
        }
    }

    protected void shutdownCommunicationPool() {
        if (communicationPool != null) {
            communicationPool.shutdown();
            try {
                if (!communicationPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    communicationPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                communicationPool.shutdownNow();
            }
        }
    }

    public void reconnectPlayer(UUID userId, ClientSession newSession) {
        for (T player : playerByNumber.values()) {
            ClientSession oldSession = player.getClientSession();
            if (oldSession != null && userId.equals(oldSession.getUserId())) {
                LOG.info("Player {} reconnecting (was disconnected for {}ms)",
                        player.getPlayerNumber(),
                        player.getDisconnectedAt() != null ?
                                System.currentTimeMillis() - player.getDisconnectedAt().toEpochMilli() : 0);
                player.setClientSession(newSession);
                player.markReconnected();
                resyncPlayer(player);
                return;
            }
        }
    }

    public void handlePlayerDisconnect(UUID userId) {
        for (T player : playerByNumber.values()) {
            if (player.getClientSession() != null && userId.equals(player.getClientSession().getUserId())) {
                LOG.info("Player {} disconnected, starting grace period", player.getPlayerNumber());
                player.markDisconnected();
                broadcastToAllPlayers(new be.lefief.sockets.commands.client.reception.DisplayChatCommand(
                        player.getClientSession().getUserName() + " disconnected. Waiting for reconnection..."));
            }
        }
        // Don't immediately check win condition - let grace period timer handle it
        scheduleGracePeriodCheck();
    }

    /**
     * Schedule a check for expired grace periods.
     */
    protected void scheduleGracePeriodCheck() {
        // Default implementation checks immediately
        // Subclasses can override for scheduled checks
        checkGracePeriods();
    }

    /**
     * Check if any disconnected players have exceeded their grace period.
     */
    protected void checkGracePeriods() {
        for (T player : playerByNumber.values()) {
            if (player.isGracePeriodExpired()) {
                LOG.info("Player {} grace period expired, treating as forfeit", player.getPlayerNumber());
                onPlayerGracePeriodExpired(player);
            }
        }
        checkWinCondition();
    }

    /**
     * Called when a player's grace period expires. Can be overridden by subclasses.
     */
    protected void onPlayerGracePeriodExpired(T player) {
        // Default: just log, checkWinCondition will handle the rest
    }

    private void checkWinCondition() {
        if (!gameIsRunning)
            return;

        long connectedCount = getActiveConnectedPlayersCount();
        if (playerByNumber.size() > 1 && connectedCount <= 1) {
            if (connectedCount == 1) {
                T winner = playerByNumber.values().stream().filter(Player::isConnected).findFirst().orElse(null);
                if (winner != null) {
                    gameIsRunning = false;
                    broadcastToAllPlayers(new be.lefief.sockets.commands.client.reception.DisplayChatCommand(
                            "Game Over! Winner by forfeit: " + winner.getClientSession().getUserName()));
                    stop();
                    if (onGameEnd != null)
                        onGameEnd.run();
                }
            } else if (connectedCount == 0) {
                gameIsRunning = false;
                stop();
                if (onGameEnd != null)
                    onGameEnd.run();
            }
        }
    }

    public long getActiveConnectedPlayersCount() {
        return playerByNumber.values().stream().filter(Player::isConnected).count();
    }
}
