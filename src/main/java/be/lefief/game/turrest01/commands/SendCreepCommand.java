package be.lefief.game.turrest01.commands;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.Map;

/**
 * Command sent by client to request sending a creep to opponents.
 */
public class SendCreepCommand extends ClientToServerCommand {

    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.GAME;
    public static final String TOPIC = "SEND_CREEP";

    public SendCreepCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    public String getCreepTypeId() {
        return (String) getData().get("creepTypeId");
    }
}
