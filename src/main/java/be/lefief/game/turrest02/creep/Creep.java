package be.lefief.game.turrest02.creep;

import lombok.Getter;

import java.awt.*;
import java.util.List;
import java.util.UUID;

@Getter
public class Creep {
    private final UUID id;
    private final CreepType type;
    private final int ownerPlayerNumber;
    private final Integer spawnedByPlayer;  // null = wave-spawned, player number = sent by that player
    private double x;
    private double y;
    private int currentPathIndex;
    private final List<Point> path;
    private int hitpoints;
    private boolean reachedCastle;

    // Slow effect tracking
    private double slowFactor = 0.0;  // 0.0 = no slow, 0.5 = 50% slow
    private long slowExpiresAt = 0;   // System time when slow expires

    public Creep(CreepType type, int ownerPlayerNumber, Integer spawnedByPlayer, List<Point> path, Point spawnerPosition) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.ownerPlayerNumber = ownerPlayerNumber;
        this.spawnedByPlayer = spawnedByPlayer;
        this.path = path;
        this.x = spawnerPosition.x + 0.5; // Center of tile
        this.y = spawnerPosition.y + 0.5;
        this.currentPathIndex = 0;
        this.hitpoints = type.getHitpoints();
        this.reachedCastle = false;
    }

    /**
     * Move the creep along the path.
     *
     * @param deltaTime Time elapsed in seconds (1.0 for one tick)
     */
    public void move(double deltaTime) {
        if (reachedCastle || path.isEmpty()) {
            return;
        }

        // Calculate effective speed with slow effect
        double effectiveSpeed = type.getTilesPerSecond();
        if (isSlowed()) {
            effectiveSpeed *= (1.0 - slowFactor);
        }

        double moveDistance = effectiveSpeed * deltaTime * 2.0; // Speed multiplier

        while (moveDistance > 0 && currentPathIndex < path.size()) {
            Point target = path.get(currentPathIndex);
            double targetX = target.x + 0.5;
            double targetY = target.y + 0.5;

            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= moveDistance) {
                // Reached this path point, move to next
                x = targetX;
                y = targetY;
                moveDistance -= distance;
                currentPathIndex++;
            } else {
                // Move toward target
                double ratio = moveDistance / distance;
                x += dx * ratio;
                y += dy * ratio;
                moveDistance = 0;
            }
        }

        // Check if reached the end of path (castle)
        if (currentPathIndex >= path.size()) {
            reachedCastle = true;
        }
    }

    public boolean hasReachedCastle() {
        return reachedCastle;
    }

    public void takeDamage(int damage) {
        hitpoints = Math.max(0, hitpoints - damage);
    }

    public boolean isDead() {
        return hitpoints <= 0;
    }

    /**
     * Apply slow effect to this creep.
     * @param factor Slow factor (0.5 = 50% speed reduction)
     * @param durationMs Duration in milliseconds
     */
    public void applySlow(double factor, int durationMs) {
        // Only apply if stronger than current slow or current slow expired
        if (factor > this.slowFactor || !isSlowed()) {
            this.slowFactor = factor;
            this.slowExpiresAt = System.currentTimeMillis() + durationMs;
        }
    }

    /**
     * Check if creep is currently slowed.
     */
    public boolean isSlowed() {
        return slowFactor > 0 && System.currentTimeMillis() < slowExpiresAt;
    }

    /**
     * Heal this creep (for healer creep ability).
     */
    public void heal(int amount) {
        this.hitpoints = Math.min(type.getHitpoints(), hitpoints + amount);
    }
}
