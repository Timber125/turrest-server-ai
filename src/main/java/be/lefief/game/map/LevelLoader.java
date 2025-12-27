package be.lefief.game.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

    private static final Logger LOG = LoggerFactory.getLogger(LevelLoader.class);

    private final TerrainType[][] terrain;
    private final int width;
    private final int height;

    private LevelLoader(TerrainType[][] terrain, int width, int height) {
        this.terrain = terrain;
        this.width = width;
        this.height = height;
    }

    public static LevelLoader load(String levelPath) throws IOException {
        LOG.info("Loading level from: {}", levelPath);

        ClassPathResource resource = new ClassPathResource(levelPath);
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }

        if (lines.isEmpty()) {
            throw new IOException("Level file is empty: " + levelPath);
        }

        int height = lines.size();
        int width = lines.get(0).length();

        TerrainType[][] terrain = new TerrainType[width][height];

        for (int y = 0; y < height; y++) {
            String row = lines.get(y);
            for (int x = 0; x < width && x < row.length(); x++) {
                terrain[x][y] = charToTerrain(row.charAt(x));
            }
        }

        LOG.info("Loaded level {}x{}", width, height);
        return new LevelLoader(terrain, width, height);
    }

    private static TerrainType charToTerrain(char c) {
        return switch (c) {
            case 'G' -> TerrainType.GRASS;
            case 'D' -> TerrainType.DIRT;
            case 'F' -> TerrainType.FOREST;
            case 'W' -> TerrainType.WATER_SHALLOW;
            case 'w' -> TerrainType.WATER_DEEP;
            case 'R' -> TerrainType.ROCKY;
            case 'C' -> TerrainType.CASTLE;
            case 'S' -> TerrainType.SPAWNER;
            default -> TerrainType.GRASS;
        };
    }

    public TerrainType[][] getTerrain() {
        return terrain;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TerrainType getTerrainAt(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return terrain[x][y];
        }
        return TerrainType.GRASS;
    }
}
