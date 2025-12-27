package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;
import lombok.Getter;

import java.util.HashMap;

@Getter
public class CountdownResponse extends ServerToClientCommand {

    public static final ClientSocketSubject SUBJECT = ClientSocketSubject.GAME;
    public static final String TOPIC = "COUNTDOWN";
    public static final String SECONDS = "seconds";

    public CountdownResponse(int seconds) {
        super(SUBJECT, TOPIC, new HashMap<String, Object>());
        getData().put(SECONDS, seconds);
    }
}
