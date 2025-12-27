package be.lefief.lobby;

import lombok.Data;

import java.util.UUID;

@Data
public class LobbyPlayer {
    private final UUID id;
    private final String name;
    private int colorIndex;
    private boolean ready;

    public LobbyPlayer(UUID id, String name, int colorIndex) {
        this.id = id;
        this.name = name;
        this.colorIndex = colorIndex;
        this.ready = false;
    }
}
