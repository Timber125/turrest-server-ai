package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class PlayerTakesDamageCommand extends ServerToClientCommand {

    public static final String TOPIC = "PLAYER_TAKES_DAMAGE";

    public PlayerTakesDamageCommand(int playerNumber, int damage, int remainingHitpoints) {
        super(ClientSocketSubject.GAME, TOPIC, createData(playerNumber, damage, remainingHitpoints));
    }

    private static Map<String, Object> createData(int playerNumber, int damage, int remainingHitpoints) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerNumber", playerNumber);
        data.put("damage", damage);
        data.put("remainingHitpoints", remainingHitpoints);
        return data;
    }
}
