package be.lefief.game;

import be.lefief.sockets.SocketHandler;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Player {

    private SocketHandler socketHandler;
    private Integer playerNumber;
    private UUID gameID;

}
