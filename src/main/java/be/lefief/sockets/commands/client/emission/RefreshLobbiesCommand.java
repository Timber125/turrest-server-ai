package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class RefreshLobbiesCommand extends ClientToServerCommand {
    public static ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static String TOPIC = "GET_ALL";
    public RefreshLobbiesCommand(){
        super(SUBJECT, TOPIC, new HashMap<>());
    }

    public RefreshLobbiesCommand(Map<String, Object> data){
        super(SUBJECT, TOPIC, data);
    }

    public RefreshLobbiesCommand(ClientToServerCommand other){
        super(other);
    }
}
