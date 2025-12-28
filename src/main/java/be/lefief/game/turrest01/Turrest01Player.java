package be.lefief.game.turrest01;

import be.lefief.game.Player;
import be.lefief.game.turrest01.resource.PlayerResources;
import be.lefief.sockets.ClientSession;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Turrest01Player extends Player {

    private static final int STARTING_HITPOINTS = 20;

    private final PlayerResources resources;
    private int hitpoints;

    public Turrest01Player(ClientSession clientSession, Integer playerNumber, UUID gameID, int colorIndex) {
        super(clientSession, playerNumber, gameID, colorIndex);
        this.resources = new PlayerResources();
        this.hitpoints = STARTING_HITPOINTS;
    }

    public void takeDamage(int damage) {
        hitpoints = Math.max(0, hitpoints - damage);
    }

    public boolean isAlive() {
        return hitpoints > 0;
    }
}
