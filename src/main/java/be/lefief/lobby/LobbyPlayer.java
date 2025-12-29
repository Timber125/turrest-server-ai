package be.lefief.lobby;

import lombok.Data;

import java.util.UUID;

@Data
public class LobbyPlayer {
    private final UUID id;
    private final String name;
    private int colorIndex;
    private boolean ready;
    private boolean bot;
    private String botDifficulty;

    public LobbyPlayer(UUID id, String name, int colorIndex) {
        this.id = id;
        this.name = name;
        this.colorIndex = colorIndex;
        this.ready = false;
        this.bot = false;
        this.botDifficulty = null;
    }

    public static LobbyPlayer createBot(String difficulty, int colorIndex) {
        LobbyPlayer bot = new LobbyPlayer(UUID.randomUUID(), "Bot (" + difficulty + ")", colorIndex);
        bot.bot = true;
        bot.botDifficulty = difficulty;
        bot.ready = true; // Bots are always ready
        return bot;
    }
}
