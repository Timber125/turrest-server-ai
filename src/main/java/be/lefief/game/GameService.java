package be.lefief.game;

import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.sockets.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GameService {

    private static final Logger LOG = LoggerFactory.getLogger(GameService.class);
    private final Map<UUID, Game> games;

    public GameService() {
        games = new HashMap<>();
    }

    public Game startGame(String gameType, List<ClientSession> lobbyPlayers) {
        LOG.info("Starting game of type '{}' with {} players", gameType, lobbyPlayers.size());

        Game game;
        if ("TURREST-mode1".equals(gameType)) {
            game = new TurrestGameMode01(lobbyPlayers);
        } else {
            LOG.warn("Unknown game type '{}', defaulting to TURREST-mode1", gameType);
            game = new TurrestGameMode01(lobbyPlayers);
        }

        games.put(game.getGameID(), game);
        game.start();

        LOG.info("Game {} started successfully", game.getGameID());
        return game;
    }

    public Game getGame(UUID gameId) {
        return games.get(gameId);
    }
}
