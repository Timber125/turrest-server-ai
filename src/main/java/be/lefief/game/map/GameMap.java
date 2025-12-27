package be.lefief.game.map;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

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
        return new GameMap(level, playerCount, Collections.emptySet(), null);
    }

    /**
     * Creates a combined game map with roads.
     *
     * @param levelPath     Path to the level file
     * @param playerCount   Number of players
     * @param roadPositions Set of positions that should have roads
     * @param roadSupplier  Supplier to create road structures
     */
    public static GameMap createFromLevelWithRoads(String levelPath, int playerCount,
                                                    Set<Point> roadPositions,
                                                    Supplier<Structure> roadSupplier) throws IOException {
        LevelLoader level = LevelLoader.load(levelPath);
        return new GameMap(level, playerCount, roadPositions, roadSupplier);
    }

    private GameMap(LevelLoader level, int playerCount, Set<Point> roadPositions,
                    Supplier<Structure> roadSupplier) {
        this.playerCount = playerCount;
        this.playerSectionWidth = level.getWidth();
        this.playerSectionHeight = level.getHeight();

        // Arrange players horizontally
        this.width = level.getWidth() * playerCount;
        this.height = level.getHeight();
        this.map = new Tile[width][height];

        LOG.info("Creating combined map {}x{} for {} players (each section {}x{})",
                width, height, playerCount, playerSectionWidth, playerSectionHeight);

        // Clone level for each player with ownership
        for (int playerNum = 0; playerNum < playerCount; playerNum++) {
            int offsetX = playerNum * playerSectionWidth;
            copyLevelToMap(level, offsetX, 0, roadPositions, roadSupplier, playerNum);
        }

        LOG.info("Combined map created successfully with {} road positions per player section",
                roadPositions.size());
    }

    private void copyLevelToMap(LevelLoader level, int offsetX, int offsetY,
                                Set<Point> roadPositions, Supplier<Structure> roadSupplier,
                                int ownerPlayerNumber) {
        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                TerrainType terrain = level.getTerrainAt(x, y);
                Tile tile = new Tile(terrain);

                // Assign ownership to this player's section
                tile.addOwner(ownerPlayerNumber);

                // Apply road if this position has one
                if (roadSupplier != null && roadPositions.contains(new Point(x, y))) {
                    tile.setStructure(roadSupplier.get());
                }

                map[offsetX + x][offsetY + y] = tile;
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
