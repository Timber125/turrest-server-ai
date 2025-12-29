package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class AddBotCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static final String TOPIC = "ADD_BOT";

    public AddBotCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    @JsonIgnore
    public String getDifficulty() {
        Object difficulty = data.get("difficulty");
        if (difficulty instanceof String) {
            return (String) difficulty;
        }
        return "EASY";
    }
}
