package be.lefief.lobby;

import java.util.*;
import java.util.stream.Collectors;

public class Lobby {

    private static final int SERVERSIDE_MAX_PLAYERS = 8;

    private boolean open;
    private final List<LobbyPlayer> players;
    private final int maxplayers;
    private final boolean hidden;
    private final String game;
    private boolean started;
    private final UUID hostId;

    public Lobby(UUID host, String hostName, int maxPlayers, boolean hidden, String game) {
        players = new ArrayList<>();
        this.maxplayers = maxPlayers;
        this.game = game;
        this.hidden = hidden;
        this.hostId = host;
        // Host gets color 0
        players.add(new LobbyPlayer(host, hostName, 0));
        open = maxPlayers > 1;
    }

    public boolean isOpen() {
        return open;
    }

    public UUID getHost() {
        return hostId;
    }

    public boolean isHidden() {
        return hidden;
    }

    public UUID getLobbyID() {
        return hostId;
    }

    public String getGame() {
        return game;
    }

    public int getSize() {
        return maxplayers;
    }

    public boolean addClient(UUID clientId, String clientName) {
        if (!isOpen()) return false;
        if (players.size() >= SERVERSIDE_MAX_PLAYERS) return false;
        if (players.size() >= maxplayers) return false;
        if (getPlayer(clientId) != null) return false;

        // Assign first available color
        int colorIndex = findNextAvailableColor();
        players.add(new LobbyPlayer(clientId, clientName, colorIndex));
        checkIfOpen();
        return true;
    }

    private int findNextAvailableColor() {
        Set<Integer> usedColors = players.stream()
                .map(LobbyPlayer::getColorIndex)
                .collect(Collectors.toSet());
        for (int i = 0; i < 16; i++) {
            if (!usedColors.contains(i)) {
                return i;
            }
        }
        return 0; // Fallback
    }

    private void checkIfOpen() {
        open = players.size() < maxplayers;
    }

    public boolean removeClient(UUID clientId) {
        if (clientId.equals(hostId)) {
            return destroyLobby("Host left the lobby");
        }
        boolean removed = players.removeIf(p -> p.getId().equals(clientId));
        if (removed) {
            open = true;
        }
        return removed;
    }

    private boolean destroyLobby(String reason) {
        open = false;
        players.clear();
        return true;
    }

    public List<UUID> getPlayerIds() {
        return players.stream()
                .map(LobbyPlayer::getId)
                .collect(Collectors.toList());
    }

    public List<LobbyPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public LobbyPlayer getPlayer(UUID playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isColorTaken(int colorIndex, UUID excludePlayerId) {
        return players.stream()
                .filter(p -> !p.getId().equals(excludePlayerId))
                .anyMatch(p -> p.getColorIndex() == colorIndex);
    }

    public boolean changePlayerColor(UUID playerId, int newColorIndex) {
        if (isColorTaken(newColorIndex, playerId)) {
            return false;
        }
        LobbyPlayer player = getPlayer(playerId);
        if (player == null || player.isReady()) {
            return false;
        }
        player.setColorIndex(newColorIndex);
        return true;
    }

    public boolean togglePlayerReady(UUID playerId) {
        LobbyPlayer player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        player.setReady(!player.isReady());
        return true;
    }

    public boolean allPlayersReady() {
        return players.stream().allMatch(LobbyPlayer::isReady);
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        started = true;
        open = false;
    }
}
