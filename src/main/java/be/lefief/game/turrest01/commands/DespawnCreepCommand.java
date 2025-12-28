package be.lefief.game.turrest01.commands;

import be.lefief.game.turrest01.creep.Creep;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class DespawnCreepCommand extends ServerToClientCommand {

    public static final String TOPIC = "DESPAWN_CREEP";

    public DespawnCreepCommand(Creep creep) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creep));
    }

    public DespawnCreepCommand(String creepId) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creepId));
    }

    private static Map<String, Object> createData(Creep creep) {
        Map<String, Object> data = new HashMap<>();
        data.put("creepId", creep.getId().toString());
        return data;
    }

    private static Map<String, Object> createData(String creepId) {
        Map<String, Object> data = new HashMap<>();
        data.put("creepId", creepId);
        return data;
    }
}
