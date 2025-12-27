package be.lefief.game.turrest01.map;

import be.lefief.game.map.LevelLoader;
import be.lefief.game.map.TerrainType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.*;

/**
 * Generates roads connecting spawners to the castle using a random-walk algorithm.
 * Roads are generated once before the map is cloned for each player.
 */
public class RoadGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(RoadGenerator.class);
    private static final double BIAS_TOWARD_CASTLE = 0.7; // 70% chance to move toward castle

    private final Random random;

    public RoadGenerator() {
        this.random = new Random();
    }

    public RoadGenerator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Generates road positions connecting all spawners to the castle.
     *
     * @param level The level to generate roads for
     * @return Set of points that should contain roads
     */
    public Set<Point> generateRoads(LevelLoader level) {
        Set<Point> roadPositions = new HashSet<>();

        // Find castle position
        Point castle = findTerrain(level, TerrainType.CASTLE);
        if (castle == null) {
            LOG.warn("No castle found in level, cannot generate roads");
            return roadPositions;
        }

        // Find all spawner positions
        List<Point> spawners = findAllTerrain(level, TerrainType.SPAWNER);
        if (spawners.isEmpty()) {
            LOG.warn("No spawners found in level, cannot generate roads");
            return roadPositions;
        }

        LOG.info("Generating roads from {} spawners to castle at ({}, {})",
                spawners.size(), castle.x, castle.y);

        // Generate a road from each spawner to the castle using BFS
        for (Point spawner : spawners) {
            Set<Point> path = generatePath(level, spawner, castle);
            roadPositions.addAll(path);
            LOG.debug("Generated road from spawner ({}, {}) to castle: {} tiles",
                    spawner.x, spawner.y, path.size());
        }

        LOG.info("Generated {} total road tiles", roadPositions.size());
        return roadPositions;
    }

    /**
     * Generates a guaranteed path from start to target using BFS.
     * Falls back to random walk if BFS fails.
     */
    private Set<Point> generatePath(LevelLoader level, Point start, Point target) {
        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        Set<Point> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (isAdjacentTo(current, target)) {
                // Reconstruct path
                Set<Point> path = reconstructPath(level, cameFrom, current);
                LOG.debug("BFS found path with {} tiles", path.size());
                return path;
            }

            for (Point neighbor : getValidNeighbors(level, current, visited)) {
                visited.add(neighbor);
                cameFrom.put(neighbor, current);
                queue.add(neighbor);
            }
        }

        LOG.warn("BFS failed to find path from ({},{}) to ({},{}), using random walk fallback",
                start.x, start.y, target.x, target.y);
        return randomWalkToTarget(level, start, target);
    }

    private List<Point> getValidNeighbors(LevelLoader level, Point current, Set<Point> visited) {
        List<Point> neighbors = new ArrayList<>();
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // N, E, S, W

        for (int[] dir : directions) {
            Point neighbor = new Point(current.x + dir[0], current.y + dir[1]);
            if (isValidMove(level, neighbor, visited)) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }

    private Set<Point> reconstructPath(LevelLoader level, Map<Point, Point> cameFrom, Point end) {
        Set<Point> path = new LinkedHashSet<>();
        Point current = end;

        while (current != null && cameFrom.get(current) != null) {
            TerrainType terrain = level.getTerrainAt(current.x, current.y);
            if (canPlaceRoad(terrain)) {
                path.add(current);
            }
            current = cameFrom.get(current);
        }

        return path;
    }

    private Point findTerrain(LevelLoader level, TerrainType target) {
        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                if (level.getTerrainAt(x, y) == target) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private List<Point> findAllTerrain(LevelLoader level, TerrainType target) {
        List<Point> positions = new ArrayList<>();
        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                if (level.getTerrainAt(x, y) == target) {
                    positions.add(new Point(x, y));
                }
            }
        }
        return positions;
    }

    private Set<Point> randomWalkToTarget(LevelLoader level, Point start, Point target) {
        Set<Point> path = new LinkedHashSet<>(); // Preserve order for debugging
        Set<Point> visited = new HashSet<>();
        Point current = new Point(start.x, start.y);

        // Don't place roads on spawner or castle
        visited.add(start);
        visited.add(target);

        int maxIterations = level.getWidth() * level.getHeight() * 2; // Safety limit
        int iterations = 0;

        while (!isAdjacentTo(current, target) && iterations < maxIterations) {
            iterations++;

            Point next = chooseNextStep(level, current, target, visited);
            if (next == null) {
                // Stuck, try to backtrack or break
                LOG.warn("Road generation stuck at ({}, {}), breaking", current.x, current.y);
                break;
            }

            // Add road at the next position (unless it's a special terrain)
            TerrainType terrain = level.getTerrainAt(next.x, next.y);
            if (canPlaceRoad(terrain)) {
                path.add(next);
            }

            visited.add(next);
            current = next;
        }

        return path;
    }

    private Point chooseNextStep(LevelLoader level, Point current, Point target, Set<Point> visited) {
        // Calculate direction toward target
        int dx = Integer.compare(target.x, current.x);
        int dy = Integer.compare(target.y, current.y);

        // Possible moves (orthogonal only)
        List<Point> preferredMoves = new ArrayList<>();
        List<Point> alternativeMoves = new ArrayList<>();

        // Add moves biased toward target
        if (dx != 0) {
            Point horizontal = new Point(current.x + dx, current.y);
            if (isValidMove(level, horizontal, visited)) {
                preferredMoves.add(horizontal);
            }
        }
        if (dy != 0) {
            Point vertical = new Point(current.x, current.y + dy);
            if (isValidMove(level, vertical, visited)) {
                preferredMoves.add(vertical);
            }
        }

        // Add orthogonal alternatives (perpendicular moves)
        for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            Point move = new Point(current.x + dir[0], current.y + dir[1]);
            if (isValidMove(level, move, visited) && !preferredMoves.contains(move)) {
                alternativeMoves.add(move);
            }
        }

        // Choose based on bias
        if (!preferredMoves.isEmpty() && random.nextDouble() < BIAS_TOWARD_CASTLE) {
            return preferredMoves.get(random.nextInt(preferredMoves.size()));
        } else if (!alternativeMoves.isEmpty()) {
            return alternativeMoves.get(random.nextInt(alternativeMoves.size()));
        } else if (!preferredMoves.isEmpty()) {
            return preferredMoves.get(random.nextInt(preferredMoves.size()));
        }

        return null;
    }

    private boolean isValidMove(LevelLoader level, Point p, Set<Point> visited) {
        if (p.x < 0 || p.x >= level.getWidth() || p.y < 0 || p.y >= level.getHeight()) {
            return false;
        }
        if (visited.contains(p)) {
            return false;
        }

        TerrainType terrain = level.getTerrainAt(p.x, p.y);
        // Can't walk through water
        return terrain != TerrainType.WATER_SHALLOW && terrain != TerrainType.WATER_DEEP;
    }

    private boolean canPlaceRoad(TerrainType terrain) {
        // Don't place roads on special tiles
        return terrain != TerrainType.CASTLE
                && terrain != TerrainType.SPAWNER
                && terrain != TerrainType.WATER_SHALLOW
                && terrain != TerrainType.WATER_DEEP;
    }

    private boolean isAdjacentTo(Point current, Point target) {
        int dx = Math.abs(current.x - target.x);
        int dy = Math.abs(current.y - target.y);
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }
}
