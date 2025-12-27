package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class ErrorMessageResponse extends ServerToClientCommand {

    public static final String TOPIC = "ERROR_MESSAGE";

    public ErrorMessageResponse(String message) {
        super(ClientSocketSubject.GAME, TOPIC, createData(message));
    }

    private static Map<String, Object> createData(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        return data;
    }
}
