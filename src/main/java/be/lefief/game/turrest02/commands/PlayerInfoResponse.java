package be.lefief.game.turrest02.commands;

import be.lefief.game.PlayerColors;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends player information including their player number and color.
 */
public class PlayerInfoResponse extends ServerToClientCommand {

    public static final String TOPIC = "PLAYER_INFO";

    public PlayerInfoResponse(int playerNumber, int colorIndex) {
        super(ClientSocketSubject.GAME, TOPIC, createData(playerNumber, colorIndex));
    }

    private static Map<String, Object> createData(int playerNumber, int colorIndex) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerNumber", playerNumber);
        data.put("colorIndex", colorIndex);
        data.put("playerColor", PlayerColors.getColor(colorIndex));
        return data;
    }
}
