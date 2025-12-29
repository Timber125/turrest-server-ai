package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.Map;

/**
 * Command sent by client to place a building on the map.
 */
public class PlaceBuildingCommand extends ClientToServerCommand {

    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.GAME;
    public static final String TOPIC = "PLACE_BUILDING";

    public PlaceBuildingCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    public int getX() {
        return ((Number) getData().get("x")).intValue();
    }

    public int getY() {
        return ((Number) getData().get("y")).intValue();
    }

    public int getBuildingType() {
        return ((Number) getData().get("buildingType")).intValue();
    }
}
