package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.Map;

/**
 * Command sent by client to request game statistics.
 */
public class GetStatsCommand extends ClientToServerCommand {

    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.GAME;
    public static final String TOPIC = "GET_STATS";

    public GetStatsCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }
}
