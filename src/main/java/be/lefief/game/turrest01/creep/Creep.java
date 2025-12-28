package be.lefief.game.turrest01.creep;

import lombok.Getter;

import java.awt.Point;
import java.util.List;
import java.util.UUID;

@Getter
public class Creep {
    private final UUID id;
    private final CreepType type;
    private final int ownerPlayerNumber;
    private double x;
    private double y;
    private int currentPathIndex;
    private final List<Point> path;
    private int hitpoints;
    private boolean reachedCastle;

    public Creep(CreepType type, int ownerPlayerNumber, List<Point> path, Point spawnerPosition) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.ownerPlayerNumber = ownerPlayerNumber;
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

        double moveDistance = type.getTilesPerSecond() * deltaTime;

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
}
