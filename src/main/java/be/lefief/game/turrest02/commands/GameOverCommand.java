package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class GameOverCommand extends ServerToClientCommand {

    public static final String TOPIC = "GAME_OVER";

    public GameOverCommand(int playerNumber, boolean isWinner) {
        super(ClientSocketSubject.GAME, TOPIC, createData(playerNumber, isWinner));
    }

    private static Map<String, Object> createData(int playerNumber, boolean isWinner) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerNumber", playerNumber);
        data.put("isWinner", isWinner);
        return data;
    }
}
