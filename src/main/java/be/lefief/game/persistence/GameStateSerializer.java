package be.lefief.game.persistence;

import be.lefief.game.map.GameMap;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest02.Turrest02Player;
import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.game.turrest02.creep.Creep;
import be.lefief.game.turrest02.resource.PlayerResources;
import be.lefief.game.turrest02.tower.Tower;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serializes and deserializes game state for persistence.
 */
@Component
public class GameStateSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(GameStateSerializer.class);

    private final ObjectMapper objectMapper;

    public GameStateSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Create a snapshot of the current game state.
     */
    public GameSnapshot createSnapshot(TurrestGameMode02 game, int tickNumber) {
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.setSnapshotId(UUID.randomUUID());
        snapshot.setGameId(game.getGameID());
        snapshot.setTickNumber(tickNumber);
        snapshot.setTimestamp(Instant.now());
        snapshot.setGameRunning(game.isGameIsRunning());

        // Snapshot players
        List<GameSnapshot.PlayerSnapshot> players = new ArrayList<>();
        for (Turrest02Player player : game.getPlayerByNumber().values()) {
            GameSnapshot.PlayerSnapshot ps = new GameSnapshot.PlayerSnapshot();
            ps.setOderId(player.getClientSession() != null ? player.getClientSession().getUserId() : null);
            ps.setPlayerNumber(player.getPlayerNumber());
            ps.setColorIndex(player.getColorIndex());
            ps.setConnected(player.isConnected());
            ps.setAlive(player.isAlive());
            ps.setHitpoints(player.getHitpoints());

            PlayerResources resources = player.getResources();
            ps.setWood(resources.getWood());
            ps.setStone(resources.getStone());
            ps.setGold(resources.getGold());
            ps.setWoodProduction(resources.getProductionRate(be.lefief.game.turrest02.resource.ResourceType.WOOD));
            ps.setStoneProduction(resources.getProductionRate(be.lefief.game.turrest02.resource.ResourceType.STONE));
            ps.setGoldProduction(resources.getProductionRate(be.lefief.game.turrest02.resource.ResourceType.GOLD));

            players.add(ps);
        }
        snapshot.setPlayers(players);

        // Snapshot structures (buildings only, roads are regenerated from level)
        List<GameSnapshot.StructureSnapshot> structures = new ArrayList<>();
        GameMap gameMap = game.getGameMap();
        if (gameMap != null) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                for (int y = 0; y < gameMap.getHeight(); y++) {
                    Tile tile = gameMap.getTile(x, y);
                    if (tile != null && tile.hasStructure()) {
                        // Skip roads - they're regenerated
                        if (tile.getStructure().getStructureTypeId() == 1) { // ROAD type ID
                            continue;
                        }
                        GameSnapshot.StructureSnapshot ss = new GameSnapshot.StructureSnapshot();
                        ss.setX(x);
                        ss.setY(y);
                        ss.setStructureTypeId(tile.getStructure().getStructureTypeId());
                        // Get owner from structure if available
                        structures.add(ss);
                    }
                }
            }
        }
        snapshot.setStructures(structures);

        // Snapshot towers
        List<GameSnapshot.TowerSnapshot> towers = new ArrayList<>();
        if (game.getTowerManager() != null) {
            for (Tower tower : game.getTowerManager().getAllTowers()) {
                GameSnapshot.TowerSnapshot ts = new GameSnapshot.TowerSnapshot();
                ts.setTowerId(tower.getId());
                ts.setX(tower.getTileX());
                ts.setY(tower.getTileY());
                ts.setTowerTypeId(tower.getDefinition().getId());
                ts.setOwnerPlayerNumber(tower.getOwnerPlayerNumber());
                ts.setCooldownRemaining(tower.getCooldownTicksRemaining());
                towers.add(ts);
            }
        }
        snapshot.setTowers(towers);

        // Snapshot creeps
        List<GameSnapshot.CreepSnapshot> creeps = new ArrayList<>();
        if (game.getCreepManager() != null) {
            for (Creep creep : game.getCreepManager().getActiveCreeps()) {
                if (creep.isDead()) continue;

                GameSnapshot.CreepSnapshot cs = new GameSnapshot.CreepSnapshot();
                cs.setCreepId(creep.getId());
                cs.setCreepTypeId(creep.getType().getId());
                cs.setOwnerPlayerNumber(creep.getOwnerPlayerNumber());
                cs.setX(creep.getX());
                cs.setY(creep.getY());
                cs.setHitpoints(creep.getHitpoints());
                cs.setCurrentPathIndex(creep.getCurrentPathIndex());
                cs.setSlowFactor(creep.getSlowFactor());
                cs.setSlowEndTime(creep.getSlowExpiresAt());
                creeps.add(cs);
            }
        }
        snapshot.setCreeps(creeps);

        return snapshot;
    }

    /**
     * Serialize snapshot to JSON.
     */
    public String serialize(GameSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize game snapshot", e);
            return null;
        }
    }

    /**
     * Deserialize snapshot from JSON.
     */
    public GameSnapshot deserialize(String json) {
        try {
            return objectMapper.readValue(json, GameSnapshot.class);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to deserialize game snapshot", e);
            return null;
        }
    }
}
