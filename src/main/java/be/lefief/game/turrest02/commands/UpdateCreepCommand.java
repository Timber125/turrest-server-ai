package be.lefief.game.turrest02.commands;

import be.lefief.game.turrest02.creep.Creep;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class UpdateCreepCommand extends ServerToClientCommand {

    public static final String TOPIC = "UPDATE_CREEP";

    public UpdateCreepCommand(Creep creep) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creep));
    }

    private static Map<String, Object> createData(Creep creep) {
        Map<String, Object> data = new HashMap<>();
        data.put("creepId", creep.getId().toString());
        data.put("x", creep.getX());
        data.put("y", creep.getY());
        data.put("hitpoints", creep.getHitpoints());
        return data;
    }
}
