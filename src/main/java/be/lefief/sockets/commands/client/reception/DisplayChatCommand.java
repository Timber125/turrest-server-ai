package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
public class DisplayChatCommand extends ServerToClientCommand {
    private static final String MESSAGE = "msg";
    public DisplayChatCommand(String message) {
        super(ClientSocketSubject.DISPLAY_CHAT, "", new HashMap<>() {{
            this.put(MESSAGE, message);
        }});
    }

    @JsonIgnore
    public String getMessage(){
        return data.get(MESSAGE).toString();
    }

}
