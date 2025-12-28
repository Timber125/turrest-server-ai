package be.lefief.game.turrest01.commands;

import be.lefief.game.turrest01.creep.Creep;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batched creep spawn command - sends all creep spawns in a single command
 * to reduce network overhead during wave spawning.
 */
public class BatchedSpawnCreepCommand extends ServerToClientCommand {

    public static final String TOPIC = "BATCHED_SPAWN_CREEP";

    public BatchedSpawnCreepCommand(Collection<Creep> creeps) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creeps));
    }

    private static Map<String, Object> createData(Collection<Creep> creeps) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> spawns = new ArrayList<>();

        for (Creep creep : creeps) {
            Map<String, Object> spawn = new HashMap<>();
            spawn.put("creepId", creep.getId().toString());
            spawn.put("creepType", creep.getType().getId());
            spawn.put("x", creep.getX());
            spawn.put("y", creep.getY());
            spawn.put("playerNumber", creep.getOwnerPlayerNumber());
            spawn.put("spawnedByPlayer", creep.getSpawnedByPlayer());
            spawn.put("hitpoints", creep.getHitpoints());
            spawn.put("maxHitpoints", creep.getType().getHitpoints());
            spawn.put("speed", creep.getType().getTilesPerSecond() * 2.0); // Include speed multiplier
            spawns.add(spawn);
        }

        data.put("spawns", spawns);
        return data;
    }
}
