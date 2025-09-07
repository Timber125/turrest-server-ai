package be.lefief.sockets.commands;

import be.lefief.sockets.SocketCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@NoArgsConstructor
@Data
@JsonSerialize
@JsonDeserialize
public class ClientToServerCommand extends SocketCommand {

    @JsonCreator
    public ClientToServerCommand(
            @JsonProperty("subject") ServerSocketSubject subject,
            @JsonProperty("topic") String topic,
            @JsonProperty("data") Map<String, Object> data
    ) {
        super(subject.name(), topic, data);
    }

    public ClientToServerCommand(
            ClientToServerCommand other
    ){
        super(other.getSubject(), other.getTopic(), other.getData());
    }

}
