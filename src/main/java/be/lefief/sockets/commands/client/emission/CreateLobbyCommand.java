package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CreateLobbyCommand extends ClientToServerCommand {

    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static final String TOPIC = "CREATE";
    private static final String LOBBY_SIZE = "size";
    private static final String HIDDEN = "hidden";
    private static final String PASSWORD = "password";
    private static final String GAME = "game";
    private static final String NAME = "name";

    public CreateLobbyCommand(
            int size,
            boolean hidden,
            String password,
            String game
    ) {
        this(size, hidden, password, game, null);
    }

    public CreateLobbyCommand(
            int size,
            boolean hidden,
            String password,
            String game,
            String name
    ) {
        super(SUBJECT, TOPIC, new HashMap<>(){{
            this.put(LOBBY_SIZE, size);
            this.put(HIDDEN, hidden);
            this.put(PASSWORD, password);
            this.put(GAME, game);
            this.put(NAME, name);
        }});
    }

    public CreateLobbyCommand(
            ServerSocketSubject subject,
            String topic,
            Map<String, Object> data
    ) {
        super(subject, topic, data);
    }

    public CreateLobbyCommand(
            ClientToServerCommand clientToServerCommand
    ) {
        super(clientToServerCommand);
    }

    public CreateLobbyCommand(Map<String, Object> data){
        super(SUBJECT, TOPIC, data);
    }

    @JsonIgnore
    public int getSize(){
        return (int) data.get(LOBBY_SIZE);
    }

    @JsonIgnore
    public String getPassword(){
        return (String) data.get(PASSWORD);
    }
    @JsonIgnore
    public boolean isHidden(){
        return (boolean) data.get(HIDDEN);
    }

    @JsonIgnore
    public String getGame(){
        return (String) data.get(GAME);
    }

    @JsonIgnore
    public String getName(){
        return (String) data.get(NAME);
    }
}
