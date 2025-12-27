package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class TileChangedResponse extends ServerToClientCommand {

    public static final String TOPIC = "TILE_CHANGED";

    public TileChangedResponse(int x, int y, int terrainTypeId) {
        super(ClientSocketSubject.GAME, TOPIC, createData(x, y, terrainTypeId));
    }

    private static Map<String, Object> createData(int x, int y, int terrainTypeId) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("newTerrainType", terrainTypeId);
        return data;
    }
}
