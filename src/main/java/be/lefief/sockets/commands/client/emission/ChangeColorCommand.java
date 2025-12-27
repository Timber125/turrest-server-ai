package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class ChangeColorCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.LOBBY;
    public static final String TOPIC = "CHANGE_COLOR";

    public ChangeColorCommand(Map<String, Object> data) {
        super(SUBJECT, TOPIC, data);
    }

    @JsonIgnore
    public int getColorIndex() {
        Object colorIndex = data.get("colorIndex");
        if (colorIndex instanceof Number) {
            return ((Number) colorIndex).intValue();
        }
        return 0;
    }
}
