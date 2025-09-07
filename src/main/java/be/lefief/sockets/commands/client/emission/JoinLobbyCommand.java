package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinLobbyCommand extends ClientToServerCommand {

    public static ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static String TOPIC = "JOIN";
    private static final String LOBBY_ID = "lobby_ID";
    private static final String HIDDEN = "hidden";
    private static final String PASSWORD = "password";
    private int size;
    private boolean hidden;
    private String password;
    public JoinLobbyCommand(
            String password,
            UUID lobbyID
    ) {
        super(SUBJECT, TOPIC, new HashMap<>(){{
            this.put(LOBBY_ID, lobbyID);
            this.put(PASSWORD, password);
        }});
    }

    public JoinLobbyCommand(ClientToServerCommand other){
        super(other);
    }

    public JoinLobbyCommand(Map<String, Object> data){
        super(SUBJECT, TOPIC, data);
    }

    @JsonIgnore
    public UUID getLobbyId(){
        return UUID.fromString((String) data.get(LOBBY_ID));
    }

}
