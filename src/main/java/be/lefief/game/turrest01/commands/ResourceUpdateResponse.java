package be.lefief.game.turrest01.commands;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class ResourceUpdateResponse extends ServerToClientCommand {

    public static final String TOPIC = "RESOURCE_UPDATE";

    public ResourceUpdateResponse(int wood, int stone, int gold) {
        super(ClientSocketSubject.GAME, TOPIC, createData(wood, stone, gold));
    }

    private static Map<String, Object> createData(int wood, int stone, int gold) {
        Map<String, Object> data = new HashMap<>();
        data.put("wood", wood);
        data.put("stone", stone);
        data.put("gold", gold);
        return data;
    }
}
