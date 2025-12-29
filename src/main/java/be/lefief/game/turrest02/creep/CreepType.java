package be.lefief.game.turrest02.creep;

import be.lefief.game.turrest02.resource.TurrestCost;
import be.lefief.game.turrest02.resource.TurrestReward;
import lombok.Getter;

@Getter
public enum CreepType {
    GHOST("GHOST", 30, 50, 1,
            new TurrestReward(0, 0, 5, 0),
            new TurrestCost(0, 0, 10, 0)),
    TROLL("TROLL", 25, 250, 2,
            new TurrestReward(0, 0, 15, 0),
            new TurrestCost(0, 0, 30, 0));

    private final String id;
    private final int speed;
    private final int hitpoints;
    private final int damage;
    private final TurrestReward killReward;
    private final TurrestCost sendCost;

    CreepType(String id, int speed, int hitpoints, int damage, TurrestReward killReward, TurrestCost sendCost) {
        this.id = id;
        this.speed = speed;
        this.hitpoints = hitpoints;
        this.damage = damage;
        this.killReward = killReward;
        this.sendCost = sendCost;
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
