package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Broadcast to all players when a building is placed.
 */
public class BuildingChangedResponse extends ServerToClientCommand {

    public static final String TOPIC = "BUILDING_CHANGED";

    public BuildingChangedResponse(int x, int y, int buildingType, int playerNumber) {
        super(ClientSocketSubject.GAME, TOPIC, createData(x, y, buildingType, playerNumber));
    }

    private static Map<String, Object> createData(int x, int y, int buildingType, int playerNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("buildingType", buildingType);
        data.put("playerNumber", playerNumber);
        return data;
    }
}
