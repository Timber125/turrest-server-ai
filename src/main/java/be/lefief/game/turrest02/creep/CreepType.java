package be.lefief.game.turrest02.creep;

import be.lefief.game.turrest02.resource.TurrestCost;
import be.lefief.game.turrest02.resource.TurrestReward;
import lombok.Getter;

@Getter
public enum CreepType {
    // Fast, weak creep - good for overwhelming single-target towers
    GHOST("GHOST", 30, 50, 1,
            new TurrestReward(0, 0, 5, 0),
            new TurrestCost(0, 0, 10, 0),
            false, 0, 0),

    // Slow, tanky creep - good against low-DPS towers
    TROLL("TROLL", 25, 250, 2,
            new TurrestReward(0, 0, 15, 0),
            new TurrestCost(0, 0, 30, 0),
            false, 0, 0),

    // Very fast, very weak - cheap and overwhelming
    RUNNER("RUNNER", 20, 25, 1,
            new TurrestReward(0, 0, 3, 0),
            new TurrestCost(0, 0, 5, 0),
            false, 0, 0),

    // Very slow, massive HP - high risk/reward
    TANK("TANK", 40, 500, 3,
            new TurrestReward(0, 0, 30, 0),
            new TurrestCost(0, 0, 60, 0),
            false, 0, 0),

    // Medium stats, heals nearby creeps - synergy creep
    HEALER("HEALER", 30, 80, 1,
            new TurrestReward(0, 0, 10, 0),
            new TurrestCost(0, 0, 25, 0),
            true, 15, 2),  // heals 15hp every 2 tiles to creeps within 1.5 tiles

    // Very fast, extremely weak - spawns in groups
    SWARM("SWARM", 25, 15, 1,
            new TurrestReward(0, 0, 2, 0),
            new TurrestCost(0, 0, 8, 0),  // cost for 5 creeps
            false, 0, 5);  // spawnCount = 5

    private final String id;
    private final int speed;
    private final int hitpoints;
    private final int damage;
    private final TurrestReward killReward;
    private final TurrestCost sendCost;

    // Special abilities
    private final boolean isHealer;
    private final int healAmount;          // HP healed to nearby creeps
    private final int spawnCount;          // Number of creeps to spawn (1 for normal, 5 for swarm)

    CreepType(String id, int speed, int hitpoints, int damage,
              TurrestReward killReward, TurrestCost sendCost,
              boolean isHealer, int healAmount, int spawnCount) {
        this.id = id;
        this.speed = speed;
        this.hitpoints = hitpoints;
        this.damage = damage;
        this.killReward = killReward;
        this.sendCost = sendCost;
        this.isHealer = isHealer;
        this.healAmount = healAmount;
        this.spawnCount = spawnCount > 0 ? spawnCount : 1;
    }

    /**
     * Get how many creeps spawn when this type is sent.
     */
    public int getSpawnCount() {
        return spawnCount;
    }

    /**
     * Check if this creep type has healing ability.
     */
    public boolean canHeal() {
        return isHealer && healAmount > 0;
    }

    /**
     * Get gold reward for killing this creep (convenience method).
     */
    public int getGoldReward() {
        return killReward.getGold();
    }

    /**
     * Get reward for when this creep hits an enemy castle.
     * Reward is sendCost gold + 5 bonus gold.
     */
    public TurrestReward getHitReward() {
        return TurrestReward.gold(sendCost.getGold() + 5);
    }

    /**
     * Get tiles per second movement rate.
     * Formula: 10 / speed
     */
    public double getTilesPerSecond() {
        return 10.0 / speed;
    }

    public static CreepType fromId(String id) {
        for (CreepType type : values()) {
            if (type.id.equalsIgnoreCase(id.trim())) {
                return type;
            }
        }
        return null;
    }
}
