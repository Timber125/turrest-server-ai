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
 * Batched creep position updates - sends all creep updates in a single command
 * to reduce network overhead and improve performance.
 */
public class BatchedCreepUpdateCommand extends ServerToClientCommand {

    public static final String TOPIC = "BATCHED_CREEP_UPDATE";

    public BatchedCreepUpdateCommand(Collection<Creep> creeps) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creeps));
    }

    private static Map<String, Object> createData(Collection<Creep> creeps) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> updates = new ArrayList<>();

        for (Creep creep : creeps) {
            Map<String, Object> update = new HashMap<>();
            update.put("id", creep.getId().toString());
            update.put("x", creep.getX());
            update.put("y", creep.getY());
            update.put("hp", creep.getHitpoints());
            updates.add(update);
        }

        data.put("updates", updates);
        return data;
    }
}
