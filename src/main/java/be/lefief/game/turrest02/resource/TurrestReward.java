package be.lefief.game.turrest02.resource;

import be.lefief.game.turrest02.Turrest02Player;
import lombok.Getter;

/**
 * Represents a reward in the Turrest game mode.
 * Can include resources (wood, stone, gold) and hitpoints.
 */
@Getter
public class TurrestReward {
    private final int wood;
    private final int stone;
    private final int gold;
    private final int hitpoints;

    public TurrestReward(int wood, int stone, int gold, int hitpoints) {
        this.wood = wood;
        this.stone = stone;
        this.gold = gold;
        this.hitpoints = hitpoints;
    }

    /**
     * Convenience constructor for resource-only rewards (no HP).
     */
    public TurrestReward(int wood, int stone, int gold) {
        this(wood, stone, gold, 0);
    }

    /**
     * Convenience constructor for gold-only rewards.
     */
    public static TurrestReward gold(int amount) {
        return new TurrestReward(0, 0, amount, 0);
    }

    /**
     * Apply this reward to a player (add resources and HP).
     */
    public void apply(Turrest02Player player) {
        PlayerResources resources = player.getResources();
        resources.add(this);
        if (hitpoints > 0) {
            player.heal(hitpoints);
        }
    }

    /**
     * Get the gold amount (convenience for simple gold rewards).
     */
    public int getGoldAmount() {
        return gold;
    }
}
