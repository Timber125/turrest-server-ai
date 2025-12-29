package be.lefief.game.turrest01.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe per-player statistics.
 * Uses AtomicInteger/AtomicLong for lock-free concurrent updates.
 */
public class PlayerStats {
    private final int playerNumber;

    private final AtomicLong goldEarned = new AtomicLong(0);
    private final AtomicLong goldSpent = new AtomicLong(0);
    private final AtomicInteger creepsKilled = new AtomicInteger(0);
    private final AtomicInteger creepsSent = new AtomicInteger(0);
    private final AtomicLong damageDealt = new AtomicLong(0);
    private final AtomicLong damageTaken = new AtomicLong(0);
    private final AtomicInteger buildingsPlaced = new AtomicInteger(0);
    private final AtomicInteger towersPlaced = new AtomicInteger(0);

    public PlayerStats(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    // Increment methods
    public void addGoldEarned(long amount) {
        goldEarned.addAndGet(amount);
    }

    public void addGoldSpent(long amount) {
        goldSpent.addAndGet(amount);
    }

    public void incrementCreepsKilled() {
        creepsKilled.incrementAndGet();
    }

    public void incrementCreepsSent() {
        creepsSent.incrementAndGet();
    }

    public void addDamageDealt(long amount) {
        damageDealt.addAndGet(amount);
    }

    public void addDamageTaken(long amount) {
        damageTaken.addAndGet(amount);
    }

    public void incrementBuildingsPlaced() {
        buildingsPlaced.incrementAndGet();
    }

    public void incrementTowersPlaced() {
        towersPlaced.incrementAndGet();
    }

    // Getters for serialization
    public int getPlayerNumber() {
        return playerNumber;
    }

    public long getGoldEarned() {
        return goldEarned.get();
    }

    public long getGoldSpent() {
        return goldSpent.get();
    }

    public int getCreepsKilled() {
        return creepsKilled.get();
    }

    public int getCreepsSent() {
        return creepsSent.get();
    }

    public long getDamageDealt() {
        return damageDealt.get();
    }

    public long getDamageTaken() {
        return damageTaken.get();
    }

    public int getBuildingsPlaced() {
        return buildingsPlaced.get();
    }

    public int getTowersPlaced() {
        return towersPlaced.get();
    }

    /**
     * Convert to serializable map for command response.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerNumber", playerNumber);
        map.put("goldEarned", goldEarned.get());
        map.put("goldSpent", goldSpent.get());
        map.put("creepsKilled", creepsKilled.get());
        map.put("creepsSent", creepsSent.get());
        map.put("damageDealt", damageDealt.get());
        map.put("damageTaken", damageTaken.get());
        map.put("buildingsPlaced", buildingsPlaced.get());
        map.put("towersPlaced", towersPlaced.get());
        return map;
    }
}
