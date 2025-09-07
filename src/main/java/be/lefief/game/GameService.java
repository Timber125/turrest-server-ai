package be.lefief.game;

import be.lefief.lobby.Lobby;
import be.lefief.sockets.SocketHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GameService {

    private final Map<UUID, Game> games;

    public GameService(){
        games = new HashMap<>();
    }

    public void startGame(List<SocketHandler> lobbyPlayers){
        Game game = new Game(lobbyPlayers);
        games.put(game.getGameID(), game);
        game.start();
    }

}
