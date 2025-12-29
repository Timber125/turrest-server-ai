package be.lefief.game.turrest02.tower;

import be.lefief.game.map.TerrainType;
import be.lefief.game.turrest02.resource.ResourceCost;
import lombok.Getter;

import java.util.List;

/**
 * Defines tower types with their stats and costs.
 * Cooldown is expressed in milliseconds and will be rounded to game ticks.
 */
@Getter
public enum TowerDefinition {

    BASIC_TOWER(1, "Basic Tower",
            3.0,           // shootingRange in tiles
            1000,          // cooldownMs (1 shot/sec theoretical)
            30,            // bulletDamage
            "BASIC",       // bulletType
            new ResourceCost(80, 80, 100),
            List.of(TerrainType.GRASS, TerrainType.DIRT),
            0.0,           // splashRadius (0 = no splash)
            0.0,           // slowFactor (0 = no slow)
            0),            // slowDurationMs

    SNIPER_TOWER(2, "Sniper Tower",
            5.0,           // shootingRange - long range
            3000,          // cooldownMs (slow fire)
            80,            // bulletDamage - high damage
            "SNIPER",      // bulletType
            new ResourceCost(60, 150, 200),
            List.of(TerrainType.GRASS, TerrainType.ROCKY),
            0.0, 0.0, 0),

    SPLASH_TOWER(3, "Splash Tower",
            2.5,           // shootingRange - medium
            1500,          // cooldownMs
            20,            // bulletDamage - area damage
            "SPLASH",      // bulletType
            new ResourceCost(120, 120, 150),
            List.of(TerrainType.GRASS, TerrainType.DIRT),
            1.0,           // splashRadius - hits all creeps in 1 tile radius
            0.0, 0),

    SLOW_TOWER(4, "Slow Tower",
            2.5,           // shootingRange - medium (buffed from 2)
            800,           // cooldownMs - fast fire
            15,            // bulletDamage - buffed from 10
            "SLOW",        // bulletType
            new ResourceCost(60, 100, 130),  // slightly adjusted cost
            List.of(TerrainType.GRASS, TerrainType.FOREST),
            0.0,
            0.5,           // slowFactor - 50% speed reduction
            2500),         // slowDurationMs - 2.5 seconds (buffed)

    RAPID_TOWER(5, "Rapid Tower",
            2.0,           // shootingRange - short
            400,           // cooldownMs (fast fire, nerfed from 300)
            12,            // bulletDamage - low damage per hit (nerfed from 15)
            "RAPID",       // bulletType
            new ResourceCost(100, 80, 100),  // slightly increased cost
            List.of(TerrainType.GRASS, TerrainType.DIRT),
            0.0, 0.0, 0);

    private final int id;
    private final String name;
    private final double shootingRange;
    private final int cooldownMs;
    private final int bulletDamage;
    private final String bulletType;
    private final ResourceCost cost;
    private final List<TerrainType> allowedTerrains;
    private final double splashRadius;
    private final double slowFactor;
    private final int slowDurationMs;

    TowerDefinition(int id, String name, double shootingRange, int cooldownMs,
                    int bulletDamage, String bulletType, ResourceCost cost,
                    List<TerrainType> allowedTerrains, double splashRadius,
                    double slowFactor, int slowDurationMs) {
        this.id = id;
        this.name = name;
        this.shootingRange = shootingRange;
        this.cooldownMs = cooldownMs;
        this.bulletDamage = bulletDamage;
        this.bulletType = bulletType;
        this.cost = cost;
        this.allowedTerrains = allowedTerrains;
        this.splashRadius = splashRadius;
        this.slowFactor = slowFactor;
        this.slowDurationMs = slowDurationMs;
    }

    /**
     * Check if this tower has splash damage.
     */
    public boolean hasSplash() {
        return splashRadius > 0;
    }

    /**
     * Check if this tower applies slow effect.
     */
    public boolean hasSlow() {
        return slowFactor > 0 && slowDurationMs > 0;
    }

    /**
     * Theoretical fire rate in shots per second.
     */
    public double getTheoreticalFireRate() {
        return 1000.0 / cooldownMs;
    }

    /**
     * Practical fire rate respecting game tick rate.
     * @param tickRateMs Game tick rate in milliseconds
     */
    public double getPracticalFireRate(int tickRateMs) {
        int cooldownTicks = getCooldownTicks(tickRateMs);
        return 1000.0 / (cooldownTicks * tickRateMs);
    }

    /**
     * Calculate cooldown in ticks (rounded up).
     */
    public int getCooldownTicks(int tickRateMs) {
        return Math.max(1, (int) Math.ceil((double) cooldownMs / tickRateMs));
    }

    /**
     * Check if this tower can be built on the given terrain type.
     */
    public boolean canBuildOn(TerrainType terrain) {
        return allowedTerrains.contains(terrain);
    }

    /**
     * Get a TowerDefinition by its ID.
     */
    public static TowerDefinition fromId(int id) {
        for (TowerDefinition def : values()) {
            if (def.id == id) {
                return def;
            }
        }
        return null;
    }
}
