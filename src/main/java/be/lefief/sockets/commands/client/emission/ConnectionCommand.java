package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectionCommand extends ClientToServerCommand {

    public static final String TOPIC = "LOGIN";
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.SOCKET_CONNECT;
    public static final String ACCESS_TOKEN = "token";
    public static final String USER_ID = "userid";

    public ConnectionCommand(String token, UUID clientID) {
        super(ServerSocketSubject.SOCKET_CONNECT, "LOGIN", new HashMap<>());
        data.put(ACCESS_TOKEN, token);
        data.put(USER_ID, clientID);
    }

    public ConnectionCommand(Map<String, Object> data) {
        super(ServerSocketSubject.SOCKET_CONNECT, "LOGIN", data);
    }
    @JsonIgnore
    public String getAccessToken(){
        return getData().get(ACCESS_TOKEN).toString();
    }

    @JsonIgnore
    public UUID getUserID(){
        return UUID.fromString(data.get(USER_ID).toString());
    }

}
