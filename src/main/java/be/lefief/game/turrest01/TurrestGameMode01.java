package be.lefief.game.turrest01;

import be.lefief.game.Game;
import be.lefief.game.map.GameMap;
import be.lefief.game.map.LevelLoader;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.commands.FullMapResponse;
import be.lefief.game.turrest01.commands.GameOverCommand;
import be.lefief.game.turrest01.commands.PlayerScoreEntry;
import be.lefief.game.turrest01.commands.ResourceUpdateResponse;
import be.lefief.game.turrest01.commands.ScoreboardCommand;
import be.lefief.game.turrest01.commands.TileUpdateResponse;
import be.lefief.game.turrest01.creep.CreepManager;
import be.lefief.game.turrest01.map.RoadGenerator;
import be.lefief.game.turrest01.tower.TowerManager;
import be.lefief.game.turrest01.resource.PlayerResources;
import be.lefief.game.turrest01.structure.Road;
import be.lefief.game.turrest01.wave.Wave;
import be.lefief.game.turrest01.wave.WaveLoader;
import be.lefief.sockets.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TurrestGameMode01 extends Game<Turrest01Player> {

    private static final Logger LOG = LoggerFactory.getLogger(TurrestGameMode01.class);
    private static final String LEVEL_PATH = "levels/0001.level";
    private static final String LEVEL_NAME = "0001";
    private static final int TICK_RATE_MS = 200; // Game tick 5 times per second (5 Hz)
    private static final double TICK_DURATION_SEC = TICK_RATE_MS / 1000.0;
    private static final int RESOURCE_UPDATE_INTERVAL = 5; // Send resource updates every 5 ticks (1 second)

    private GameMap gameMap;
    private CreepManager creepManager;
    private TowerManager towerManager;
    private ScheduledExecutorService gameLoop;
    private boolean running;
    private int tickCount = 0;
    private int resourceTickCounter = 0;

    public TurrestGameMode01(List<ClientSession> players, UUID lobbyHostId, Map<UUID, Integer> playerColorMap) {
        super(players, lobbyHostId, playerColorMap);
    }

    @Override
    protected Turrest01Player createPlayer(ClientSession session, int playerNumber, UUID gameId, int colorIndex) {
        return new Turrest01Player(session, playerNumber, gameId, colorIndex);
    }

    @Override
    public void start() {
        LOG.info("Starting TurrestGameMode01 for {} players with 5s countdown", getPlayerByNumber().size());

        try {
            // 1. Send countdown to players
            broadcastToAllPlayers(new be.lefief.sockets.commands.client.reception.CountdownResponse(5));

            // 2. Load level and generate roads
            int playerCount = getPlayerByNumber().size();
            LevelLoader level = LevelLoader.load(LEVEL_PATH);

            // Generate roads from spawners to castle (same for all players)
            RoadGenerator roadGenerator = new RoadGenerator();
            Set<Point> roadPositions = roadGenerator.generateRoads(level);

            // 3. Create combined map with roads
            gameMap = GameMap.createFromLevelWithRoads(LEVEL_PATH, playerCount, roadPositions, Road::new);
            LOG.info("Created combined map {}x{} for {} players with {} roads per section",
                    gameMap.getWidth(), gameMap.getHeight(), playerCount, roadPositions.size());

            // 4. Load waves and create CreepManager
            List<Wave> waves = WaveLoader.load(LEVEL_NAME);
            creepManager = new CreepManager(waves, gameMap, playerCount);
            LOG.info("Loaded {} waves for creep spawning", waves.size());

            // 5. Create TowerManager
            towerManager = new TowerManager(creepManager, TICK_RATE_MS);
            LOG.info("TowerManager created with tick rate {}ms", TICK_RATE_MS);

            // 5. Schedule game beginning after 5 seconds
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
    protected void resyncPlayer(Turrest01Player player) {
        if (gameMap == null || player.getClientSession() == null)
            return;
        LOG.info("Resyncing player {}", player.getPlayerNumber());

        // Send full map with structure data
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    player.getClientSession().sendCommand(new TileUpdateResponse(x, y, tile));
                }
            }
        }

        // Send current resources - no instanceof needed!
        PlayerResources resources = player.getResources();
        player.getClientSession().sendCommand(new ResourceUpdateResponse(
                resources.getWood(),
                resources.getStone(),
                resources.getGold()
        ));
    }

    private void sendInitialMapToPlayers() {
        LOG.info("Sending initial map to all players");

        // First, send player info to each player so they know their number and color
        for (Turrest01Player player : getPlayerByNumber().values()) {
            if (player.isConnected()) {
                player.getClientSession().sendCommand(
                        new be.lefief.game.turrest01.commands.PlayerInfoResponse(player.getPlayerNumber(), player.getColorIndex()));
            }
        }

        // Build playerNumber -> colorIndex mapping for contour rendering
        Map<Integer, Integer> playerColorMap = new HashMap<>();
        for (Turrest01Player player : getPlayerByNumber().values()) {
            playerColorMap.put(player.getPlayerNumber(), player.getColorIndex());
        }

        // Send entire map in one command (much faster than tile-by-tile)
        broadcastToAllPlayers(new FullMapResponse(gameMap, playerColorMap));
        LOG.info("Sent full map ({}x{}) to all players in single command",
                gameMap.getWidth(), gameMap.getHeight());

        // Send initial resources to each player
        for (Turrest01Player player : getPlayerByNumber().values()) {
            if (player.isConnected()) {
                PlayerResources resources = player.getResources();
                player.getClientSession().sendCommand(new ResourceUpdateResponse(
                        resources.getWood(),
                        resources.getStone(),
                        resources.getGold()
                ));
            }
        }

        // Send initial scoreboard
        broadcastScoreboard();
    }

    /**
     * Broadcast current scoreboard to all players.
     * Called after game start and whenever a player's score changes.
     */
    public void broadcastScoreboard() {
        List<PlayerScoreEntry> entries = getPlayerByNumber().values().stream()
                .sorted(Comparator.comparingInt(Turrest01Player::getScore).reversed())
                .map(p -> new PlayerScoreEntry(
                        p.getPlayerNumber(),
                        p.getColorIndex(),
                        p.getClientSession() != null ? p.getClientSession().getUserName() : "Player " + p.getPlayerNumber(),
                        p.getScore(),
                        p.isAlive()
                ))
                .collect(Collectors.toList());

        broadcastToAllPlayers(new ScoreboardCommand(entries));
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public TowerManager getTowerManager() {
        return towerManager;
    }

    public int getTickRateMs() {
        return TICK_RATE_MS;
    }

    /**
     * Award gold to a player (e.g., for killing creeps).
     * Sends a resource update to the player.
     */
    public void awardGoldToPlayer(int playerNumber, int gold) {
        Turrest01Player player = getPlayerByNumber().get(playerNumber);
        if (player != null && player.isConnected() && player.isAlive()) {
            PlayerResources resources = player.getResources();
            resources.addGold(gold);
            sendResourceUpdateToPlayer(playerNumber);
        }
    }

    /**
     * Send resource update to a specific player.
     */
    public void sendResourceUpdateToPlayer(int playerNumber) {
        Turrest01Player player = getPlayerByNumber().get(playerNumber);
        if (player != null && player.isConnected()) {
            PlayerResources resources = player.getResources();
            player.getClientSession().sendCommand(new ResourceUpdateResponse(
                    resources.getWood(),
                    resources.getStone(),
                    resources.getGold()
            ));
        }
    }

    /**
     * Send a command to a specific player by player number.
     */
    public void sendToPlayer(int playerNumber, be.lefief.sockets.commands.ServerToClientCommand command) {
        Turrest01Player player = getPlayerByNumber().get(playerNumber);
        if (player != null && player.isConnected()) {
            player.getClientSession().sendCommand(command);
        }
    }

    public CreepManager getCreepManager() {
        return creepManager;
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
            resourceTickCounter++;

            // Process creeps (spawn, move, damage) with delta time
            if (creepManager != null) {
                creepManager.tick(tickCount, this, TICK_DURATION_SEC);
            }

            // Process towers (targeting, shooting)
            if (towerManager != null) {
                towerManager.tick(this);
            }

            // Process resource production and updates every RESOURCE_UPDATE_INTERVAL ticks (1 second at 5Hz)
            if (resourceTickCounter >= RESOURCE_UPDATE_INTERVAL) {
                resourceTickCounter = 0;

                for (Turrest01Player player : getPlayerByNumber().values()) {
                    if (player.isConnected() && player.isAlive()) {
                        PlayerResources resources = player.getResources();
                        resources.addProduction();

                        // Send resource update to the player
                        player.getClientSession().sendCommand(new ResourceUpdateResponse(
                                resources.getWood(),
                                resources.getStone(),
                                resources.getGold()
                        ));
                    }
                }
            }

            if (tickCount % 25 == 0) { // Log every 5 seconds (25 ticks at 5Hz)
                LOG.debug("Game tick {} completed, active creeps: {}",
                        tickCount, creepManager != null ? creepManager.getActiveCreepCount() : 0);
            }
        } catch (Exception e) {
            LOG.error("Error in game tick", e);
        }
    }

    public void handlePlayerDeath(Turrest01Player player) {
        LOG.info("Player {} has been eliminated!", player.getPlayerNumber());
        broadcastScoreboard();
        broadcastToAllPlayers(new GameOverCommand(player.getPlayerNumber(), false));

        // Check if game is over (only one player left)
        long alivePlayers = getPlayerByNumber().values().stream()
                .filter(Turrest01Player::isAlive)
                .count();

        if (alivePlayers <= 1) {
            Turrest01Player winner = getPlayerByNumber().values().stream()
                    .filter(Turrest01Player::isAlive)
                    .findFirst()
                    .orElse(null);

            if (winner != null) {
                LOG.info("Game over! Player {} wins!", winner.getPlayerNumber());
                broadcastToAllPlayers(new GameOverCommand(winner.getPlayerNumber(), true));
            }

            stop();
        }
    }

    @Override
    public void handlePlayerDisconnect(java.util.UUID userId) {
        // Find the disconnecting player
        Turrest01Player disconnectedPlayer = null;
        for (Turrest01Player player : getPlayerByNumber().values()) {
            if (player.getClientSession() != null && userId.equals(player.getClientSession().getUserId())) {
                disconnectedPlayer = player;
                player.setConnected(false);
                break;
            }
        }

        if (!isGameIsRunning()) return;

        // Check for winner by forfeit
        long connectedCount = getActiveConnectedPlayersCount();
        if (getPlayerByNumber().size() > 1 && connectedCount == 1) {
            Turrest01Player winner = getPlayerByNumber().values().stream()
                    .filter(Turrest01Player::isConnected)
                    .findFirst()
                    .orElse(null);

            if (winner != null) {
                LOG.info("Game over! Player {} wins by forfeit (opponent disconnected)", winner.getPlayerNumber());

                // Send game over to disconnected player (loser)
                if (disconnectedPlayer != null) {
                    broadcastToAllPlayers(new GameOverCommand(disconnectedPlayer.getPlayerNumber(), false));
                }

                // Send winner announcement
                broadcastToAllPlayers(new GameOverCommand(winner.getPlayerNumber(), true));

                stop();
                if (getOnGameEnd() != null) {
                    getOnGameEnd().run();
                }
            }
        } else if (connectedCount == 0) {
            setGameIsRunning(false);
            stop();
            if (getOnGameEnd() != null) {
                getOnGameEnd().run();
            }
        }
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
        shutdownCommunicationPool();
        LOG.info("Game loop stopped after {} ticks", tickCount);
    }
}
