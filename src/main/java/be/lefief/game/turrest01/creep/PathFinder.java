package be.lefief.game.turrest01.creep;

import be.lefief.game.map.GameMap;
import be.lefief.game.map.TerrainType;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.structure.Road;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.*;

/**
 * Finds paths from spawners to castles using BFS on road tiles.
 */
public class PathFinder {

    private static final Logger LOG = LoggerFactory.getLogger(PathFinder.class);

    /**
     * Compute paths from spawner to castle for each player.
     *
     * @param gameMap The game map
     * @param playerCount Number of players
     * @return Map of playerNumber -> ordered path from spawner to castle
     */
    public static Map<Integer, List<Point>> computePlayerPaths(GameMap gameMap, int playerCount) {
        Map<Integer, List<Point>> playerPaths = new HashMap<>();

        for (int playerNum = 0; playerNum < playerCount; playerNum++) {
            int offsetX = gameMap.getPlayerOffsetX(playerNum);
            int sectionWidth = gameMap.getPlayerSectionWidth();
            int sectionHeight = gameMap.getPlayerSectionHeight();

            // Find spawner and castle in this player's section
            Point spawner = findTerrain(gameMap, offsetX, sectionWidth, sectionHeight, TerrainType.SPAWNER);
            Point castle = findTerrain(gameMap, offsetX, sectionWidth, sectionHeight, TerrainType.CASTLE);

            if (spawner == null || castle == null) {
                LOG.warn("Player {} missing spawner or castle", playerNum);
                playerPaths.put(playerNum, new ArrayList<>());
                continue;
            }

            List<Point> path = findPath(gameMap, spawner, castle);
            playerPaths.put(playerNum, path);
            LOG.info("Player {} path: {} tiles from ({},{}) to ({},{})",
                    playerNum, path.size(), spawner.x, spawner.y, castle.x, castle.y);
        }

        return playerPaths;
    }

    /**
     * Get the spawner position for a player.
     */
    public static Point getSpawnerPosition(GameMap gameMap, int playerNumber) {
        int offsetX = gameMap.getPlayerOffsetX(playerNumber);
        int sectionWidth = gameMap.getPlayerSectionWidth();
        int sectionHeight = gameMap.getPlayerSectionHeight();
        return findTerrain(gameMap, offsetX, sectionWidth, sectionHeight, TerrainType.SPAWNER);
    }

    private static Point findTerrain(GameMap gameMap, int offsetX, int sectionWidth, int sectionHeight, TerrainType target) {
        for (int x = offsetX; x < offsetX + sectionWidth; x++) {
            for (int y = 0; y < sectionHeight; y++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null && tile.getTerrainType() == target) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    /**
     * Find path from spawner to castle using BFS on roads.
     * The path includes road tiles in order from spawner toward castle.
     */
    private static List<Point> findPath(GameMap gameMap, Point spawner, Point castle) {
        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        Set<Point> visited = new HashSet<>();

        // Start from spawner's adjacent road tiles
        for (Point neighbor : getNeighbors(spawner)) {
            Tile tile = gameMap.getTile(neighbor.x, neighbor.y);
            if (tile != null && isWalkable(tile)) {
                queue.add(neighbor);
                visited.add(neighbor);
                cameFrom.put(neighbor, spawner);
            }
        }

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            // Check if reached the castle tile
            if (current.equals(castle)) {
                return reconstructPath(cameFrom, current, spawner);
            }

            for (Point neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;

                Tile tile = gameMap.getTile(neighbor.x, neighbor.y);
                if (tile != null && isWalkable(tile)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        LOG.warn("No path found from spawner to castle");
        return new ArrayList<>();
    }

    private static boolean isWalkable(Tile tile) {
        // Can walk on roads
        if (tile.getStructure() instanceof Road) {
            return true;
        }
        // Can also walk on castle/spawner terrain
        return tile.getTerrainType() == TerrainType.CASTLE ||
               tile.getTerrainType() == TerrainType.SPAWNER;
    }

    private static List<Point> getNeighbors(Point p) {
        return Arrays.asList(
            new Point(p.x, p.y - 1),  // North
            new Point(p.x + 1, p.y),  // East
            new Point(p.x, p.y + 1),  // South
            new Point(p.x - 1, p.y)   // West
        );
    }

    private static boolean isAdjacent(Point a, Point b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }

    private static List<Point> reconstructPath(Map<Point, Point> cameFrom, Point end, Point start) {
        List<Point> path = new ArrayList<>();
        Point current = end;

        while (current != null && !current.equals(start)) {
            path.add(current);
            current = cameFrom.get(current);
        }

        // Reverse to get path from spawner to castle
        Collections.reverse(path);
        return path;
    }
}
