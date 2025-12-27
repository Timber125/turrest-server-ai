package be.lefief.game.map;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Getter
public class GameMap {

    private static final Logger LOG = LoggerFactory.getLogger(GameMap.class);

    private final Tile[][] map;
    private final int width;
    private final int height;
    private final int playerCount;
    private final int playerSectionWidth;
    private final int playerSectionHeight;

    /**
     * Creates a combined game map by cloning a level for each player.
     * Players are arranged horizontally side by side.
     */
    public static GameMap createFromLevel(String levelPath, int playerCount) throws IOException {
        LevelLoader level = LevelLoader.load(levelPath);
        return new GameMap(level, playerCount);
    }

    private GameMap(LevelLoader level, int playerCount) {
        this.playerCount = playerCount;
        this.playerSectionWidth = level.getWidth();
        this.playerSectionHeight = level.getHeight();

        // Arrange players horizontally
        this.width = level.getWidth() * playerCount;
        this.height = level.getHeight();
        this.map = new Tile[width][height];

        LOG.info("Creating combined map {}x{} for {} players (each section {}x{})",
                width, height, playerCount, playerSectionWidth, playerSectionHeight);

        // Clone level for each player
        for (int playerNum = 0; playerNum < playerCount; playerNum++) {
            int offsetX = playerNum * playerSectionWidth;
            copyLevelToMap(level, offsetX, 0);
        }

        LOG.info("Combined map created successfully");
    }

    private void copyLevelToMap(LevelLoader level, int offsetX, int offsetY) {
        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                TerrainType terrain = level.getTerrainAt(x, y);
                map[offsetX + x][offsetY + y] = new Tile(terrain);
            }
        }
    }

    /**
     * Gets the X offset for a player's section of the map.
     */
    public int getPlayerOffsetX(int playerNumber) {
        return playerNumber * playerSectionWidth;
    }

    /**
     * Gets the Y offset for a player's section of the map.
     */
    public int getPlayerOffsetY(int playerNumber) {
        return 0; // Players arranged horizontally
    }

    public Tile getTile(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return map[x][y];
        }
        return null;
    }
}
