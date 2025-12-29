package be.lefief.game.ai;

import be.lefief.game.ai.strategy.EasyBotStrategy;
import be.lefief.game.turrest02.Turrest02Player;
import be.lefief.game.turrest02.TurrestGameMode02;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages bot players in a game.
 */
public class BotManager {

    private static final Logger LOG = LoggerFactory.getLogger(BotManager.class);

    private final Map<Integer, BotStrategy> botStrategies = new HashMap<>();

    /**
     * Register a bot player with a strategy.
     */
    public void registerBot(int playerNumber, BotDifficulty difficulty) {
        BotStrategy strategy = createStrategy(difficulty);
        botStrategies.put(playerNumber, strategy);
        LOG.info("Registered bot for player {} with {} difficulty", playerNumber, difficulty);
    }

    /**
     * Called each game tick to let bots make decisions.
     */
    public void tick(TurrestGameMode02 game, int tickCount) {
        for (Map.Entry<Integer, BotStrategy> entry : botStrategies.entrySet()) {
            int playerNumber = entry.getKey();
            BotStrategy strategy = entry.getValue();

            Turrest02Player player = game.getPlayerByNumber().get(playerNumber);
            if (player != null && player.isAlive()) {
                try {
                    strategy.think(game, player, tickCount);
                } catch (Exception e) {
                    LOG.error("Bot {} error during think", playerNumber, e);
                }
            }
        }
    }

    /**
     * Check if a player is a bot.
     */
    public boolean isBot(int playerNumber) {
        return botStrategies.containsKey(playerNumber);
    }

    /**
     * Get the number of registered bots.
     */
    public int getBotCount() {
        return botStrategies.size();
    }

    private BotStrategy createStrategy(BotDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new EasyBotStrategy();
            case MEDIUM -> new EasyBotStrategy(); // TODO: MediumBotStrategy
            case HARD -> new EasyBotStrategy();   // TODO: HardBotStrategy
        };
    }

    public enum BotDifficulty {
        EASY, MEDIUM, HARD
    }
}
