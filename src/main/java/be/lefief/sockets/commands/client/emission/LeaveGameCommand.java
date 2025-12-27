package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class LeaveGameCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.GAME;
    public static final String TOPIC = "LEAVE_GAME";

    public LeaveGameCommand() {
        super(SUBJECT, TOPIC, new HashMap<>());
    }

    public LeaveGameCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    public LeaveGameCommand(ClientToServerCommand other) {
        super(other);
    }
}
