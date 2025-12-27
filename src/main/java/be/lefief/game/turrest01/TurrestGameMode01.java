package be.lefief.game.turrest01;

import be.lefief.game.Game;
import be.lefief.game.Player;
import be.lefief.game.map.GameMap;
import be.lefief.game.map.TerrainType;
import be.lefief.game.map.Tile;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.commands.client.reception.TileChangedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TurrestGameMode01 extends Game {

    private static final Logger LOG = LoggerFactory.getLogger(TurrestGameMode01.class);
    private static final String LEVEL_PATH = "levels/0001.level";
    private static final int TICK_RATE_MS = 1000; // Game tick every second

    private GameMap gameMap;
    private ScheduledExecutorService gameLoop;
    private boolean running;
    private final Random random = new Random();
    private int tickCount = 0;

    public TurrestGameMode01(List<ClientSession> players, UUID lobbyHostId) {
        super(players, lobbyHostId);
    }

    @Override
    public void start() {
        LOG.info("Starting TurrestGameMode01 for {} players with 5s countdown", getPlayerByNumber().size());

        try {
            // 1. Send countdown to players
            broadcastToAllPlayers(new be.lefief.sockets.commands.client.reception.CountdownResponse(5));

            // 2. Load level and create combined map for all players
            int playerCount = getPlayerByNumber().size();
            gameMap = GameMap.createFromLevel(LEVEL_PATH, playerCount);
            LOG.info("Created combined map {}x{} for {} players",
                    gameMap.getWidth(), gameMap.getHeight(), playerCount);

            // 3. Schedule game beginning after 5 seconds
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                if (isGameIsRunning()) {
                    LOG.info("Countdown finished, sending map and starting game loop");
                    sendInitialMapToPlayers();
                    startGameLoop();
                }
            }, 5, TimeUnit.SECONDS);

        } catch (IOException e) {
            LOG.error("Failed to load level: {}", LEVEL_PATH, e);
        }
    }

    @Override
    protected void resyncPlayer(Player player) {
        if (gameMap == null || player.getClientSession() == null)
            return;
        LOG.info("Resyncing player {}", player.getPlayerNumber());

        // Send full map
        // Optimization TODO: Send in bulk command instead of individual tile updates
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    TileChangedResponse tileUpdate = new TileChangedResponse(
                            x, y, tile.getTerrainType().getTerrainTypeID());
                    player.getClientSession().sendCommand(tileUpdate);
                }
            }
        }
    }

    private void sendInitialMapToPlayers() {
        LOG.info("Sending initial map to all players");
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    TileChangedResponse tileUpdate = new TileChangedResponse(
                            x, y, tile.getTerrainType().getTerrainTypeID());
                    broadcastToAllPlayers(tileUpdate);
                }
            }
        }
        LOG.info("Finished sending {} tiles to players", gameMap.getWidth() * gameMap.getHeight());
    }

    private void startGameLoop() {
        running = true;
        gameLoop = Executors.newSingleThreadScheduledExecutor();
        gameLoop.scheduleAtFixedRate(this::gameTick, TICK_RATE_MS, TICK_RATE_MS, TimeUnit.MILLISECONDS);
        LOG.info("Game loop started with tick rate of {}ms", TICK_RATE_MS);
    }

    private void gameTick() {
        if (!running || gameMap == null || !isGameIsRunning())
            return;

        try {
            tickCount++;

            // Example game logic: randomly change a few tiles each tick
            int tilesToChange = random.nextInt(3) + 1; // 1-3 tiles per tick
            for (int i = 0; i < tilesToChange; i++) {
                int x = random.nextInt(gameMap.getWidth());
                int y = random.nextInt(gameMap.getHeight());
                TerrainType newTerrain = getRandomTerrain();

                Tile tile = gameMap.getTile(x, y);
                if (tile != null && tile.getTerrainType() != newTerrain) {
                    tile.setTerrainType(newTerrain);
                    broadcastToAllPlayers(new TileChangedResponse(x, y, newTerrain.getTerrainTypeID()));
                }
            }

            if (tickCount % 10 == 0) {
                LOG.debug("Game tick {} completed", tickCount);
            }
        } catch (Exception e) {
            LOG.error("Error in game tick", e);
        }
    }

    private TerrainType getRandomTerrain() {
        TerrainType[] types = TerrainType.values();
        return types[random.nextInt(types.length)];
    }

    @Override
    public void stop() {
        running = false;
        setGameIsRunning(false);
        if (gameLoop != null) {
            gameLoop.shutdown();
            try {
                if (!gameLoop.awaitTermination(5, TimeUnit.SECONDS)) {
                    gameLoop.shutdownNow();
                }
            } catch (InterruptedException e) {
                gameLoop.shutdownNow();
            }
        }
        LOG.info("Game loop stopped after {} ticks", tickCount);
    }
}
