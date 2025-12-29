package be.lefief.game.ai;

import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.game.turrest02.Turrest02Player;

/**
 * Strategy interface for bot AI decision-making.
 */
public interface BotStrategy {

    /**
     * Called each game tick to make decisions.
     * @param game The game state
     * @param player The bot's player
     * @param tickCount Current game tick
     */
    void think(TurrestGameMode02 game, Turrest02Player player, int tickCount);

    /**
     * Get the difficulty name.
     */
    String getDifficultyName();
}
