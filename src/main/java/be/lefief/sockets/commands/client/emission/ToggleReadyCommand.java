package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.Map;

public class ToggleReadyCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static final String TOPIC = "TOGGLE_READY";

    public ToggleReadyCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }
}
