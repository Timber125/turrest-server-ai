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
    private boolean connected = true;

    // Constructor adaptation if AllArgsConstructor is used elsewhere
    public Player(ClientSession clientSession, Integer playerNumber, UUID gameID) {
        this.clientSession = clientSession;
        this.playerNumber = playerNumber;
        this.gameID = gameID;
        this.connected = true;
    }
}
