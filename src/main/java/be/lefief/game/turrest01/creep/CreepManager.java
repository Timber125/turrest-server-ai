package be.lefief.game.turrest01.creep;

import be.lefief.game.map.GameMap;
import be.lefief.game.turrest01.Turrest01Player;
import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.game.turrest01.commands.BatchedCreepUpdateCommand;
import be.lefief.game.turrest01.commands.BatchedSpawnCreepCommand;
import be.lefief.game.turrest01.commands.DespawnCreepCommand;
import be.lefief.game.turrest01.commands.PlayerTakesDamageCommand;
import be.lefief.game.turrest01.wave.Wave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CreepManager {

    private static final Logger LOG = LoggerFactory.getLogger(CreepManager.class);

    private final Map<UUID, Creep> activeCreeps = new ConcurrentHashMap<>();
    private final List<Wave> waves;
    private final Map<Integer, List<Point>> playerPaths;
    private final Map<Integer, Point> playerSpawners;
    private final int playerCount;

    public CreepManager(List<Wave> waves, GameMap gameMap, int playerCount) {
        this.waves = waves;
        this.playerCount = playerCount;
        this.playerPaths = PathFinder.computePlayerPaths(gameMap, playerCount);
        this.playerSpawners = new HashMap<>();

        // Store spawner positions for each player
        for (int i = 0; i < playerCount; i++) {
            Point spawner = PathFinder.getSpawnerPosition(gameMap, i);
            if (spawner != null) {
                playerSpawners.put(i, spawner);
            }
        }

        LOG.info("CreepManager initialized with {} waves for {} players", waves.size(), playerCount);
    }

    /**
     * Process one game tick.
     *
     * @param currentTick Current tick number
     * @param game        Game instance for broadcasting
     * @param deltaTime   Time elapsed since last tick in seconds
     */
    public void tick(int currentTick, TurrestGameMode01 game, double deltaTime) {
        // 1. Spawn wave creeps if it's time
        spawnWaveCreeps(currentTick, game);

        // 2. Move all creeps
        moveCreeps(game, deltaTime);

        // 3. Check for creeps reaching castle
        checkCastleReached(game);
    }

    private void spawnWaveCreeps(int tick, TurrestGameMode01 game) {
        List<Creep> spawnedCreeps = new ArrayList<>();

        for (Wave wave : waves) {
            if (wave.getTick() == tick) {
                LOG.info("Spawning wave at tick {}: {} creeps per player", tick, wave.getCreeps().size());

                // Spawn creeps for each player
                for (int playerNum = 0; playerNum < playerCount; playerNum++) {
                    List<Point> path = playerPaths.get(playerNum);
                    Point spawner = playerSpawners.get(playerNum);

                    if (path == null || path.isEmpty() || spawner == null) {
                        LOG.warn("Player {} has no valid path, skipping wave spawn", playerNum);
                        continue;
                    }

                    for (CreepType type : wave.getCreeps()) {
                        // null = wave-spawned (no colored contour)
                        Creep creep = new Creep(type, playerNum, null, path, spawner);
                        activeCreeps.put(creep.getId(), creep);
                        spawnedCreeps.add(creep);
                    }
                }
            }
        }

        // Send all spawns in a single batched command
        if (!spawnedCreeps.isEmpty()) {
            game.broadcastToAllPlayers(new BatchedSpawnCreepCommand(spawnedCreeps));
            LOG.debug("Batched {} creep spawns into single command", spawnedCreeps.size());
        }
    }

    private void moveCreeps(TurrestGameMode01 game, double deltaTime) {
        List<Creep> movedCreeps = new ArrayList<>();

        for (Creep creep : activeCreeps.values()) {
            if (!creep.hasReachedCastle() && !creep.isDead()) {
                creep.move(deltaTime);
                movedCreeps.add(creep);
            }
        }

        // Send all updates in a single batched command
        if (!movedCreeps.isEmpty()) {
            game.broadcastToAllPlayers(new BatchedCreepUpdateCommand(movedCreeps));
        }
    }

    private void checkCastleReached(TurrestGameMode01 game) {
        Iterator<Map.Entry<UUID, Creep>> it = activeCreeps.entrySet().iterator();

        while (it.hasNext()) {
            Creep creep = it.next().getValue();

            if (creep.hasReachedCastle()) {
                // Deal damage to player
                Turrest01Player player = game.getPlayerByNumber().get(creep.getOwnerPlayerNumber());
                if (player != null) {
                    int damage = creep.getType().getDamage();
                    player.takeDamage(damage);

                    LOG.info("Creep {} reached castle, dealing {} damage to player {} (HP: {})",
                            creep.getId(), damage, player.getPlayerNumber(), player.getHitpoints());

                    // Broadcast commands
                    game.broadcastToAllPlayers(new DespawnCreepCommand(creep));
                    game.broadcastToAllPlayers(new PlayerTakesDamageCommand(
                            player.getPlayerNumber(),
                            damage,
                            player.getHitpoints()
                    ));

                    // Update scoreboard (score = HP changed)
                    game.broadcastScoreboard();

                    // Check for player death
                    if (!player.isAlive()) {
                        game.handlePlayerDeath(player);
                    }
                }

                it.remove();
            } else if (creep.isDead()) {
                // Creep was killed by towers - award kill reward to the player who owns this section
                int playerNum = creep.getOwnerPlayerNumber();
                Turrest01Player player = game.getPlayerByNumber().get(playerNum);
                if (player != null) {
                    creep.getType().getKillReward().apply(player);
                }

                int goldReward = creep.getType().getGoldReward();
                LOG.debug("Creep {} killed, awarding {} gold to player {}",
                        creep.getId(), goldReward, playerNum);

                game.broadcastToAllPlayers(new DespawnCreepCommand(creep, goldReward, playerNum));
                game.sendResourceUpdateToPlayer(playerNum);
                it.remove();
            }
        }
    }

    /**
     * Spawn a creep sent by a player to all OTHER players.
     * The creep will have a colored contour indicating who sent it.
     *
     * @param type              The type of creep to spawn
     * @param senderPlayerNumber The player who sent the creep
     * @param game              Game instance for broadcasting
     */
    public void spawnSentCreep(CreepType type, int senderPlayerNumber, TurrestGameMode01 game) {
        List<Creep> spawnedCreeps = new ArrayList<>();

        // Spawn creep for all players EXCEPT the sender
        for (int playerNum = 0; playerNum < playerCount; playerNum++) {
            if (playerNum == senderPlayerNumber) {
                continue; // Don't spawn on sender's own section
            }

            List<Point> path = playerPaths.get(playerNum);
            Point spawner = playerSpawners.get(playerNum);

            if (path == null || path.isEmpty() || spawner == null) {
                LOG.warn("Player {} has no valid path, skipping sent creep spawn", playerNum);
                continue;
            }

            // spawnedByPlayer = sender's number (for colored contour)
            Creep creep = new Creep(type, playerNum, senderPlayerNumber, path, spawner);
            activeCreeps.put(creep.getId(), creep);
            spawnedCreeps.add(creep);
        }

        // Broadcast spawns
        if (!spawnedCreeps.isEmpty()) {
            game.broadcastToAllPlayers(new BatchedSpawnCreepCommand(spawnedCreeps));
            LOG.info("Player {} sent {} to {} opponents",
                    senderPlayerNumber, type.getId(), spawnedCreeps.size());
        }
    }

    public int getActiveCreepCount() {
        return activeCreeps.size();
    }

    public Collection<Creep> getActiveCreeps() {
        return Collections.unmodifiableCollection(activeCreeps.values());
    }
}
