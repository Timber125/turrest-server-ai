package be.lefief.lobby;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class Lobby {

    private static final int SERVERSIDE_MAX_PLAYERS = 8;

    private boolean open;
    private final UUID[] players;
    private final int maxplayers;
    private final boolean hidden;
    private final String game;
    private boolean started;

    public Lobby(UUID host, int maxPlayers, boolean hidden, String game) {
        players = new UUID[SERVERSIDE_MAX_PLAYERS];
        this.maxplayers = maxPlayers;
        this.game = game;
        this.hidden = hidden;
        players[0] = host;
        open = maxPlayers > 1;
    }

    public boolean isOpen() {
        return open;
    }

    public UUID getHost() {
        return players[0];
    }

    public boolean isHidden() {
        return hidden;
    }

    public UUID getLobbyID() {
        // currently same as playerid
        return players[0];
    }

    public String getGame() {
        return game;
    }

    public int getSize() {
        return maxplayers;
    }

    public boolean addClient(UUID clientId) {
        if (!isOpen()) return false;
        for (int i = 1; i < players.length; i++) {
            if (players[i] == null) {
                players[i] = clientId;
                checkIfOpen();
                return true;
            }
        }
        return false;
    }

    private void checkIfOpen() {
        for (int i = 0; i < maxplayers; i++) {
            if (players[i] == null) {
                this.open = true;
                return;
            }
        }
        open = false;
    }

    public boolean removeClient(UUID clientId) {
        if (clientId == players[0]) return destroyLobby("Host left the lobby");
        for (int i = 1; i < players.length; i++) {
            if (players[i] == clientId) {
                players[i] = null;
                open = true;
                return true;
            }
        }
        return false;
    }

    private boolean destroyLobby(String reason) {
        open = false;
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) {
                kick(i, players[i], reason);
            }
        }
        return true;
    }

    private void kick(int playerNumber, UUID player, String reason) {
        // ?
        players[playerNumber] = null;
    }

    public List<UUID> getPlayers() {
        return Arrays.stream(players)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        started = true;
        open = false;
    }
}
