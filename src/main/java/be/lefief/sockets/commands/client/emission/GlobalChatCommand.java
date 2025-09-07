package be.lefief.sockets.commands.client.emission;

import be.lefief.sockets.SocketCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class GlobalChatCommand extends ClientToServerCommand {

    public static ServerSocketSubject SUBJECT = ServerSocketSubject.GLOBAL_CHAT;
    public static String TOPIC = "GLOBAL";
    public static final String MESSAGE = "msg";
    public GlobalChatCommand(String message) {
        super(SUBJECT, TOPIC, new HashMap<>() {{
            this.put(MESSAGE, message);
        }});
    }

    public GlobalChatCommand(Map<String, Object> data){
        super(SUBJECT, TOPIC, data);
    }
    @JsonIgnore
    public String getMessage(){
        return data.get(MESSAGE).toString();
    }

}
