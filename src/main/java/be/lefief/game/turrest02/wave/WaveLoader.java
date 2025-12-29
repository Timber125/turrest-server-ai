package be.lefief.game.turrest02.wave;

import be.lefief.game.turrest02.creep.CreepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WaveLoader {

    private static final Logger LOG = LoggerFactory.getLogger(WaveLoader.class);

    /**
     * Load waves from a .waves file.
     * Format: "tick: CREEP_TYPE, CREEP_TYPE, ..."
     * Example: "30: GHOST, GHOST, GHOST"
     *
     * @param levelName The level name (e.g., "0001")
     * @return List of waves sorted by tick
     */
    public static List<Wave> load(String levelName) throws IOException {
        String path = "levels/" + levelName + ".waves";
        List<Wave> waves = new ArrayList<>();

        try (InputStream is = WaveLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                LOG.warn("Wave file not found: {}", path);
                return waves;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    Wave wave = parseLine(line, lineNumber);
                    if (wave != null) {
                        waves.add(wave);
                    }
                }
            }
        }

        // Sort by tick
        waves.sort((a, b) -> Integer.compare(a.getTick(), b.getTick()));

        LOG.info("Loaded {} waves from {}", waves.size(), path);
        return waves;
    }

    private static Wave parseLine(String line, int lineNumber) {
        // Format: "tick: TYPE, TYPE, TYPE"
        String[] parts = line.split(":", 2);
        if (parts.length != 2) {
            LOG.warn("Invalid wave format at line {}: {}", lineNumber, line);
            return null;
        }

        try {
            int tick = Integer.parseInt(parts[0].trim());
            String[] creepIds = parts[1].split(",");
            List<CreepType> creeps = new ArrayList<>();

            for (String creepId : creepIds) {
                CreepType type = CreepType.fromId(creepId.trim());
                if (type != null) {
                    creeps.add(type);
                } else {
                    LOG.warn("Unknown creep type '{}' at line {}", creepId.trim(), lineNumber);
                }
            }

            if (creeps.isEmpty()) {
                LOG.warn("No valid creeps at line {}", lineNumber);
                return null;
            }

            return new Wave(tick, creeps);

        } catch (NumberFormatException e) {
            LOG.warn("Invalid tick number at line {}: {}", lineNumber, parts[0]);
            return null;
        }
    }
}
