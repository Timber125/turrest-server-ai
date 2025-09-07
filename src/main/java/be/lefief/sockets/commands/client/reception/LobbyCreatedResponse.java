package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.UUID;

public class LobbyCreatedResponse extends ServerToClientCommand {
    private static final String LOBBY_ID = "lobbyId";
    private static final String LOBBY_SIZE = "size";
    private static final String HIDDEN = "hidden";
    private static final String PASSWORD = "password";
    private static final String GAME = "game";
    public LobbyCreatedResponse(UUID lobbyID, Integer lobbySize, boolean hidden, String password, String game) {
        super(ClientSocketSubject.LOBBY, "CREATED", new HashMap<>() {{
            this.put(LOBBY_ID, lobbyID);
            this.put(LOBBY_SIZE, lobbySize);
            this.put(HIDDEN, hidden);
            this.put(PASSWORD, password);
            this.put(GAME, game);
        }});
    }
    @JsonIgnore
    public UUID getLobbyId(){
        return UUID.fromString(data.get(LOBBY_ID).toString());
    }

}
