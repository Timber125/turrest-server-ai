package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;
import java.util.UUID;

public class RemoveBotCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static final String TOPIC = "REMOVE_BOT";

    public RemoveBotCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    @JsonIgnore
    public UUID getBotId() {
        Object botId = data.get("botId");
        if (botId instanceof String) {
            return UUID.fromString((String) botId);
        }
        return null;
    }
}
