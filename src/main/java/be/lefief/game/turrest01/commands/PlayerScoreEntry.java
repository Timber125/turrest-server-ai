package be.lefief.game.turrest01.commands;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerScoreEntry {
    private int playerNumber;
    private int colorIndex;
    private String username;
    private int score;
    private boolean alive;
}
