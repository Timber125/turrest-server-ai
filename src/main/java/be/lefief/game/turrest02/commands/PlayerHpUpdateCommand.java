package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to sync a player's HP (used for reconnection).
 */
public class PlayerHpUpdateCommand extends ServerToClientCommand {

    public static final String TOPIC = "PLAYER_HP_UPDATE";

    public PlayerHpUpdateCommand(int playerNumber, int hitpoints) {
        super(ClientSocketSubject.GAME, TOPIC, createData(playerNumber, hitpoints));
    }

    private static Map<String, Object> createData(int playerNumber, int hitpoints) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerNumber", playerNumber);
        data.put("hitpoints", hitpoints);
        return data;
    }
}
