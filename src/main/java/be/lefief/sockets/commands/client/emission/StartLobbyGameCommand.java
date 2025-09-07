package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class StartLobbyGameCommand extends ClientToServerCommand {

    public static ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static String TOPIC = "START_GAME";

    public StartLobbyGameCommand(){
        super(SUBJECT, TOPIC, new HashMap<>(){{

        }});
    }
    public StartLobbyGameCommand(Map<String, Object> data){
        super(SUBJECT, TOPIC, data);
    }

}
