package be.lefief.game;

import be.lefief.sockets.ClientSession;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Player {

    private ClientSession clientSession;
    private Integer playerNumber;
    private UUID gameID;

}
