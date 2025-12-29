package be.lefief.game.turrest02.tower;

import lombok.Getter;

import java.util.UUID;

/**
 * Base class for all tower types.
 * Towers automatically shoot at creeps within range.
 */
@Getter
public abstract class Tower {

    private final UUID id;
    private final int ownerPlayerNumber;
    private final int tileX;
    private final int tileY;
    private int cooldownTicksRemaining;

    public Tower(int ownerPlayerNumber, int tileX, int tileY) {
        this.id = UUID.randomUUID();
        this.ownerPlayerNumber = ownerPlayerNumber;
        this.tileX = tileX;
        this.tileY = tileY;
        this.cooldownTicksRemaining = 0;
    }

    // Abstract methods - implemented by specific tower types
    public abstract TowerDefinition getDefinition();

    // Convenience getters delegating to definition
    public double getShootingRange() {
        return getDefinition().getShootingRange();
    }

    public int getCooldownMs() {
        return getDefinition().getCooldownMs();
    }

    public int getBulletDamage() {
        return getDefinition().getBulletDamage();
    }

    public String getBulletType() {
        return getDefinition().getBulletType();
    }

    /**
     * Calculate cooldown in ticks based on tick rate (rounded up).
     */
    public int getCooldownTicks(int tickRateMs) {
        return Math.max(1, (int) Math.ceil((double) getCooldownMs() / tickRateMs));
    }

    /**
     * Theoretical fire rate in shots per second.
     */
    public double getTheoreticalFireRate() {
        return 1000.0 / getCooldownMs();
    }

    /**
     * Practical fire rate respecting game tick rate.
     */
    public double getPracticalFireRate(int tickRateMs) {
        int cooldownTicks = getCooldownTicks(tickRateMs);
        return 1000.0 / (cooldownTicks * tickRateMs);
    }

    /**
     * Check if tower can fire (cooldown expired).
     */
    public boolean canFire() {
        return cooldownTicksRemaining <= 0;
    }

    /**
     * Fire the tower, resetting cooldown based on tick rate.
     */
    public void fire(int tickRateMs) {
        cooldownTicksRemaining = getCooldownTicks(tickRateMs);
    }

    /**
     * Decrement cooldown by one tick.
     */
    public void tickCooldown() {
        cooldownTicksRemaining = Math.max(0, cooldownTicksRemaining - 1);
    }

    /**
     * Get center X position of tower (for distance calculations).
     */
    public double getCenterX() {
        return tileX + 0.5;
    }

    /**
     * Get center Y position of tower (for distance calculations).
     */
    public double getCenterY() {
        return tileY + 0.5;
    }
}
