package be.lefief.game.turrest01.creep;

import be.lefief.game.map.GameMap;
import be.lefief.game.turrest01.Turrest01Player;
import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.game.turrest01.commands.DespawnCreepCommand;
import be.lefief.game.turrest01.commands.PlayerTakesDamageCommand;
import be.lefief.game.turrest01.commands.SpawnCreepCommand;
import be.lefief.game.turrest01.commands.UpdateCreepCommand;
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
     */
    public void tick(int currentTick, TurrestGameMode01 game) {
        // 1. Spawn wave creeps if it's time
        spawnWaveCreeps(currentTick, game);

        // 2. Move all creeps
        moveCreeps(game);

        // 3. Check for creeps reaching castle
        checkCastleReached(game);
    }

    private void spawnWaveCreeps(int tick, TurrestGameMode01 game) {
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
                        game.broadcastToAllPlayers(new SpawnCreepCommand(creep));
                    }
                }
            }
        }
    }

    private void moveCreeps(TurrestGameMode01 game) {
        for (Creep creep : activeCreeps.values()) {
            if (!creep.hasReachedCastle() && !creep.isDead()) {
                creep.move(1.0); // 1 second per tick
                game.broadcastToAllPlayers(new UpdateCreepCommand(creep));
            }
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
                // Creep was killed by turrets (future feature)
                game.broadcastToAllPlayers(new DespawnCreepCommand(creep));
                it.remove();
            }
        }
    }

    public int getActiveCreepCount() {
        return activeCreeps.size();
    }

    public Collection<Creep> getActiveCreeps() {
        return Collections.unmodifiableCollection(activeCreeps.values());
    }
}
