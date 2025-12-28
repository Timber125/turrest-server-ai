package be.lefief.game.turrest01.commands;

import be.lefief.game.turrest01.creep.Creep;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class SpawnCreepCommand extends ServerToClientCommand {

    public static final String TOPIC = "SPAWN_CREEP";

    public SpawnCreepCommand(Creep creep) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creep));
    }

    private static Map<String, Object> createData(Creep creep) {
        Map<String, Object> data = new HashMap<>();
        data.put("creepId", creep.getId().toString());
        data.put("creepType", creep.getType().getId());
        data.put("x", creep.getX());
        data.put("y", creep.getY());
        data.put("playerNumber", creep.getOwnerPlayerNumber());
        data.put("hitpoints", creep.getHitpoints());
        data.put("maxHitpoints", creep.getType().getHitpoints());
        data.put("speed", creep.getType().getTilesPerSecond());
        return data;
    }
}
