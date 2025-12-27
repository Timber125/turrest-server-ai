package be.lefief.game.turrest01;

import be.lefief.game.Player;
import be.lefief.game.turrest01.resource.PlayerResources;
import be.lefief.sockets.ClientSession;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Turrest01Player extends Player {

    private final PlayerResources resources;

    public Turrest01Player(ClientSession clientSession, Integer playerNumber, UUID gameID, int colorIndex) {
        super(clientSession, playerNumber, gameID, colorIndex);
        this.resources = new PlayerResources();
    }
}
