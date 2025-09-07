package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class GameStartedResponse extends ServerToClientCommand {
    public static ClientSocketSubject SUBJECT = ClientSocketSubject.LOBBY;
    public static String TOPIC = "GAME_STARTED";

    public GameStartedResponse(){
        this(new HashMap<>(){{

        }});
    }
    public GameStartedResponse(Map<String, Object> data){
        super(SUBJECT, TOPIC, data);
    }

}
