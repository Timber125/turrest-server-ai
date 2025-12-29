package be.lefief.game.turrest01.stats;

import be.lefief.game.turrest01.event.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe game statistics collector.
 * Subscribes to game events and aggregates statistics.
 */
public class GameStats {
    private final Map<Integer, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final long gameStartTime = System.currentTimeMillis();

    /**
     * Process a game event and update stats accordingly.
     * Central entry point for all stat recording.
     */
    public void recordEvent(TurrestEvent event) {
        int player = event.getPlayerNumber();
        PlayerStats stats = playerStats.computeIfAbsent(player, PlayerStats::new);

        if (event instanceof CreepKilledEvent e) {
            stats.incrementCreepsKilled();
            stats.addGoldEarned(e.getGoldReward());
        } else if (event instanceof CreepSentEvent e) {
            stats.incrementCreepsSent();
            stats.addGoldSpent(e.getGoldCost());
        } else if (event instanceof BuildingBuiltEvent) {
            stats.incrementBuildingsPlaced();
        } else if (event instanceof TowerBuiltEvent) {
            stats.incrementTowersPlaced();
        } else if (event instanceof DamageDealtEvent e) {
            stats.addDamageDealt(e.getDamage());
        }
    }

    /**
     * Record damage taken by a player (from creeps reaching castle).
     */
    public void recordDamageTaken(int playerNumber, int damage) {
        PlayerStats stats = playerStats.computeIfAbsent(playerNumber, PlayerStats::new);
        stats.addDamageTaken(damage);
    }

    /**
     * Get stats for a specific player.
     */
    public PlayerStats getStats(int playerNumber) {
        return playerStats.getOrDefault(playerNumber, new PlayerStats(playerNumber));
    }

    /**
     * Get game duration in milliseconds.
     */
    public long getGameDurationMs() {
        return System.currentTimeMillis() - gameStartTime;
    }

    /**
     * Get all player stats (immutable view).
     */
    public Map<Integer, PlayerStats> getAllPlayerStats() {
        return Collections.unmodifiableMap(playerStats);
    }
}
