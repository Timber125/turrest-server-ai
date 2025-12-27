package be.lefief.game;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.commands.ServerToClientCommand;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class Game {
    private Map<Integer, Player> playerByNumber;
    private UUID gameID;
    private UUID lobbyHostId;

    @Setter
    private boolean gameIsRunning = true;

    @Setter
    private Runnable onGameEnd;

    public Game(List<ClientSession> players, UUID lobbyHostId) {
        playerByNumber = new HashMap<>();
        gameID = UUID.randomUUID();
        this.lobbyHostId = lobbyHostId;
        for (int i = 0; i < players.size(); i++) {
            playerByNumber.put(i, new Player(players.get(i), i, gameID));
        }
    }

    public abstract void start();

    public abstract void stop();

    protected abstract void resyncPlayer(Player player);

    protected void broadcastToAllPlayers(ServerToClientCommand command) {
        playerByNumber.values().forEach(player -> player.getClientSession().sendCommand(command));
    }

    public void reconnectPlayer(UUID userId, ClientSession newSession) {
        for (Player player : playerByNumber.values()) {
            ClientSession oldSession = player.getClientSession();
            if (oldSession != null && userId.equals(oldSession.getClientID())) {
                player.setClientSession(newSession);
                player.setConnected(true);
                resyncPlayer(player);
                return;
            }
        }
    }

    public void handlePlayerDisconnect(UUID userId) {
        for (Player p : playerByNumber.values()) {
            if (p.getClientSession() != null && userId.equals(p.getClientSession().getClientID())) {
                p.setConnected(false);
            }
        }
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (!gameIsRunning)
            return;

        long connectedCount = getActiveConnectedPlayersCount();
        if (playerByNumber.size() > 1 && connectedCount <= 1) {
            if (connectedCount == 1) {
                Player winner = playerByNumber.values().stream().filter(Player::isConnected).findFirst().orElse(null);
                if (winner != null) {
                    gameIsRunning = false;
                    // TODO: Send EndGameCommand or similar
                    broadcastToAllPlayers(new be.lefief.sockets.commands.client.reception.DisplayChatCommand(
                            "Game Over! Winner by forfeit: " + winner.getClientSession().getClientName()));
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
