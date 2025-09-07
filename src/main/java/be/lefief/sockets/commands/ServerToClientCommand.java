package be.lefief.sockets.commands;

import be.lefief.sockets.SocketCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@JsonSerialize
@NoArgsConstructor
@Data
public class ServerToClientCommand extends SocketCommand {

    @JsonCreator
    public ServerToClientCommand(ClientSocketSubject subject, String topic, Map<String, Object> data) {
        super(subject.name(), topic, data);
    }

}

