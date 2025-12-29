package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class RenameLobbyCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static final String TOPIC = "RENAME";

    public RenameLobbyCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    @JsonIgnore
    public String getNewName() {
        Object name = data.get("name");
        if (name instanceof String) {
            return (String) name;
        }
        return null;
    }
}
