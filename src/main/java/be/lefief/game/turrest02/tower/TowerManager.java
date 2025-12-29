package be.lefief.game.turrest02.tower;

import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.game.turrest02.commands.BatchedTowerAttackCommand;
import be.lefief.game.turrest02.creep.Creep;
import be.lefief.game.turrest02.creep.CreepManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all towers in the game.
 * Handles tower targeting, firing, and cooldowns.
 */
public class TowerManager {

    private static final Logger LOG = LoggerFactory.getLogger(TowerManager.class);

    private final Map<UUID, Tower> towers = new ConcurrentHashMap<>();
    private final CreepManager creepManager;
    private final int tickRateMs;

    public TowerManager(CreepManager creepManager, int tickRateMs) {
        this.creepManager = creepManager;
        this.tickRateMs = tickRateMs;
        LOG.info("TowerManager initialized with tick rate {}ms", tickRateMs);
    }

    /**
     * Register a new tower.
     */
    public void addTower(Tower tower) {
        towers.put(tower.getId(), tower);
        LOG.debug("Tower {} added at ({}, {}) for player {}",
                tower.getId(), tower.getTileX(), tower.getTileY(), tower.getOwnerPlayerNumber());
    }

    /**
     * Remove a tower (e.g., when destroyed or sold).
     */
    public void removeTower(UUID towerId) {
        Tower removed = towers.remove(towerId);
        if (removed != null) {
            LOG.debug("Tower {} removed", towerId);
        }
    }

    /**
     * Get a tower by ID.
     */
    public Tower getTower(UUID towerId) {
        return towers.get(towerId);
    }

    /**
     * Get all towers.
     */
    public Collection<Tower> getAllTowers() {
        return Collections.unmodifiableCollection(towers.values());
    }

    /**
     * Get towers owned by a specific player.
     */
    public List<Tower> getTowersByPlayer(int playerNumber) {
        List<Tower> playerTowers = new ArrayList<>();
        for (Tower tower : towers.values()) {
            if (tower.getOwnerPlayerNumber() == playerNumber) {
                playerTowers.add(tower);
            }
        }
        return playerTowers;
    }

    /**
     * Process one game tick - update cooldowns, find targets, fire.
     */
    public void tick(TurrestGameMode02 game) {
        List<TowerAttack> attacks = new ArrayList<>();

        for (Tower tower : towers.values()) {
            // Decrement cooldown
            tower.tickCooldown();

            // Check if tower can fire
            if (tower.canFire()) {
                Creep target = findTarget(tower);
                if (target != null) {
                    // Fire!
                    tower.fire(tickRateMs);
                    target.takeDamage(tower.getBulletDamage());
                    attacks.add(new TowerAttack(tower, target));

                    LOG.trace("Tower {} fired at creep {}, dealt {} damage (HP: {})",
                            tower.getId(), target.getId(), tower.getBulletDamage(), target.getHitpoints());
                }
            }
        }

        // Broadcast all attacks in single batched command
        if (!attacks.isEmpty()) {
            game.broadcastToAllPlayers(new BatchedTowerAttackCommand(attacks));
        }
    }

    /**
     * Find target using FURTHEST strategy:
     * Target the creep closest to the castle (highest path index) that is within range.
     * Only targets creeps on the tower owner's section.
     */
    private Creep findTarget(Tower tower) {
        Creep bestTarget = null;
        int highestPathIndex = -1;

        for (Creep creep : creepManager.getActiveCreeps()) {
            // Skip dead creeps
            if (creep.isDead()) {
                continue;
            }

            // Only target creeps on this tower's owner's section
            if (creep.getOwnerPlayerNumber() != tower.getOwnerPlayerNumber()) {
                continue;
            }

            // Check if in range
            if (!isInRange(tower, creep)) {
                continue;
            }

            // FURTHEST strategy: highest path index = closest to castle
            if (creep.getCurrentPathIndex() > highestPathIndex) {
                highestPathIndex = creep.getCurrentPathIndex();
                bestTarget = creep;
            }
        }

        return bestTarget;
    }

    /**
     * Check if a creep is within tower's shooting range.
     */
    private boolean isInRange(Tower tower, Creep creep) {
        double dx = tower.getCenterX() - creep.getX();
        double dy = tower.getCenterY() - creep.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= tower.getShootingRange();
    }

    /**
     * Get tower count.
     */
    public int getTowerCount() {
        return towers.size();
    }
}
