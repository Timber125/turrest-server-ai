package be.lefief.game.turrest01.resource;

import be.lefief.game.turrest01.Turrest01Player;
import lombok.Getter;

/**
 * Represents a cost in the Turrest game mode.
 * Can include resources (wood, stone, gold) and hitpoints.
 */
@Getter
public class TurrestCost {
    private final int wood;
    private final int stone;
    private final int gold;
    private final int hitpoints;

    public TurrestCost(int wood, int stone, int gold, int hitpoints) {
        this.wood = wood;
        this.stone = stone;
        this.gold = gold;
        this.hitpoints = hitpoints;
    }

    /**
     * Convenience constructor for resource-only costs (no HP).
     */
    public TurrestCost(int wood, int stone, int gold) {
        this(wood, stone, gold, 0);
    }

    /**
     * Check if the player can afford this cost.
     */
    public boolean canAfford(PlayerResources resources, int playerHitpoints) {
        return resources.getWood() >= wood
                && resources.getStone() >= stone
                && resources.getGold() >= gold
                && playerHitpoints > hitpoints; // Must have MORE than cost (can't kill yourself)
    }

    /**
     * Check if player can afford (convenience method using player object).
     */
    public boolean canAfford(Turrest01Player player) {
        return canAfford(player.getResources(), player.getHitpoints());
    }

    /**
     * Apply this cost to a player (subtract resources and HP).
     */
    public void apply(Turrest01Player player) {
        PlayerResources resources = player.getResources();
        resources.subtract(this);
        if (hitpoints > 0) {
            player.takeDamage(hitpoints);
        }
    }

    /**
     * Convert to legacy ResourceCost (for backwards compatibility).
     */
    public ResourceCost toResourceCost() {
        return new ResourceCost(wood, stone, gold);
    }
}
