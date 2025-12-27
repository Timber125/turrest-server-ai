package be.lefief.game;

import be.lefief.sockets.ClientSession;
import lombok.Data;

import java.util.UUID;

@Data
public class Player {

    private ClientSession clientSession;
    private Integer playerNumber;
    private UUID gameID;
    private boolean connected = true;
    private int colorIndex;

    public Player(ClientSession clientSession, Integer playerNumber, UUID gameID, int colorIndex) {
        this.clientSession = clientSession;
        this.playerNumber = playerNumber;
        this.gameID = gameID;
        this.connected = true;
        this.colorIndex = colorIndex;
    }
}
