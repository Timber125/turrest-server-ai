package be.lefief.game.turrest01.creep;

import lombok.Getter;

@Getter
public enum CreepType {
    GHOST("GHOST", 30, 50, 1),
    TROLL("TROLL", 25, 250, 2);

    private final String id;
    private final int speed;
    private final int hitpoints;
    private final int damage;

    CreepType(String id, int speed, int hitpoints, int damage) {
        this.id = id;
        this.speed = speed;
        this.hitpoints = hitpoints;
        this.damage = damage;
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
