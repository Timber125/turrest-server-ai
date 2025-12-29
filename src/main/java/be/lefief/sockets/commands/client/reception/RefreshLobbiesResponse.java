package be.lefief.sockets.commands.client.reception;

import be.lefief.lobby.Lobby;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;
import be.lefief.sockets.commands.client.emission.RefreshLobbiesCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class RefreshLobbiesResponse extends ServerToClientCommand {
    public static final String NUMBER_OF_LOBBIES = "numberOfLobbies";
    private static final String LOBBY_ID = "lobby_id";
    private static final String LOBBY_SIZE = "size";
    private static final String HIDDEN = "hidden";
    private static final String PASSWORD = "password";
    private static final String GAME = "game";
    private static final String NAME = "name";

    public static String LOBBY_ID(int i) { return LOBBY_ID + i;}
    public static String LOBBY_SIZE(int i) { return LOBBY_SIZE + i;}
    public static String HIDDEN(int i) { return HIDDEN + i;}
    public static String PASSWORD(int i) { return PASSWORD + i;}
    public static String GAME(int i) { return GAME + i;}
    public static String NAME(int i) { return NAME + i;}
    public RefreshLobbiesResponse(Map<String, Object> data){
        super(ClientSocketSubject.LOBBY, "DATA:ALL_LOBBIES", data);
    }
    @JsonIgnore
    public UUID getLobbyId(){
        return UUID.fromString(data.get(LOBBY_ID).toString());
    }

}
