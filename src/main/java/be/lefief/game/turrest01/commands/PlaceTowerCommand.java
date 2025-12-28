package be.lefief.game.turrest01.commands;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.Map;

/**
 * Command sent by client to place a tower on the map.
 */
public class PlaceTowerCommand extends ClientToServerCommand {

    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.GAME;
    public static final String TOPIC = "PLACE_TOWER";

    public PlaceTowerCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    public int getX() {
        return ((Number) getData().get("x")).intValue();
    }

    public int getY() {
        return ((Number) getData().get("y")).intValue();
    }

    public int getTowerType() {
        return ((Number) getData().get("towerType")).intValue();
    }
}
