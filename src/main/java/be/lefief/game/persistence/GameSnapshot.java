package be.lefief.game.persistence;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serializable snapshot of game state for persistence and replay.
 */
@Data
public class GameSnapshot {

    private UUID snapshotId;
    private UUID gameId;
    private int tickNumber;
    private Instant timestamp;

    // Player states
    private List<PlayerSnapshot> players;

    // Map state (only structures, terrain is static)
    private List<StructureSnapshot> structures;

    // Tower states
    private List<TowerSnapshot> towers;

    // Active creep states
    private List<CreepSnapshot> creeps;

    // Game metadata
    private String levelPath;
    private boolean gameRunning;

    @Data
    public static class PlayerSnapshot {
        private UUID oderId;
        private int playerNumber;
        private int colorIndex;
        private boolean connected;
        private boolean alive;
        private int hitpoints;
        private int wood;
        private int stone;
        private int gold;
        private int woodProduction;
        private int stoneProduction;
        private int goldProduction;
    }

    @Data
    public static class StructureSnapshot {
        private int x;
        private int y;
        private int structureTypeId;
        private int ownerPlayerNumber;
    }

    @Data
    public static class TowerSnapshot {
        private UUID towerId;
        private int x;
        private int y;
        private int towerTypeId;
        private int ownerPlayerNumber;
        private int cooldownRemaining;
    }

    @Data
    public static class CreepSnapshot {
        private UUID creepId;
        private String creepTypeId;
        private int ownerPlayerNumber;
        private double x;
        private double y;
        private int hitpoints;
        private int currentPathIndex;
        private double slowFactor;
        private long slowEndTime;
    }
}
