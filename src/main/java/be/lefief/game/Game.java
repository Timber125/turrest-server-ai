package be.lefief.game;

import be.lefief.sockets.SocketHandler;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class Game {
    private Map<Integer, Player> playerByNumber;
    private UUID gameID;
    public Game(List<SocketHandler> players){
        playerByNumber = new HashMap<>();
        gameID = UUID.randomUUID();
        for(int i = 0; i < players.size(); i++){
            playerByNumber.put(i, new Player(players.get(i), i, gameID));
        }
    }

    public void start() {

    }
}
