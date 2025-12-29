package be.lefief.game.ai.strategy;

import be.lefief.game.ai.BotStrategy;
import be.lefief.game.map.GameMap;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest02.Turrest02Player;
import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.game.turrest02.building.BuildingDefinition;
import be.lefief.game.turrest02.creep.CreepType;
import be.lefief.game.turrest02.resource.PlayerResources;
import be.lefief.game.turrest02.structure.TurrestBuilding;
import be.lefief.game.turrest02.tower.GenericTower;
import be.lefief.game.turrest02.tower.TowerDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Easy difficulty bot - makes random decisions with delays.
 */
public class EasyBotStrategy implements BotStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EasyBotStrategy.class);
    private static final int DECISION_INTERVAL_TICKS = 25; // Decide every 5 seconds

    private final Random random = new Random();

    @Override
    public void think(TurrestGameMode02 game, Turrest02Player player, int tickCount) {
        // Only make decisions periodically
        if (tickCount % DECISION_INTERVAL_TICKS != 0) {
            return;
        }

        PlayerResources resources = player.getResources();
        int action = random.nextInt(10);

        if (action < 4) {
            // 40% chance: Try to build a tower
            tryBuildTower(game, player, resources);
        } else if (action < 7) {
            // 30% chance: Try to build a building
            tryBuildBuilding(game, player, resources);
        } else {
            // 30% chance: Try to send a creep
            trySendCreep(game, player, resources);
        }
    }

    private void tryBuildTower(TurrestGameMode02 game, Turrest02Player player, PlayerResources resources) {
        // Pick a random tower type we can afford
        List<TowerDefinition> affordable = new ArrayList<>();
        for (TowerDefinition def : TowerDefinition.values()) {
            if (resources.canAfford(def.getCost())) {
                affordable.add(def);
            }
        }

        if (affordable.isEmpty()) {
            return;
        }

        TowerDefinition selectedTower = affordable.get(random.nextInt(affordable.size()));

        // Find a valid tile to build on
        Tile tile = findBuildableTile(game, player, selectedTower);
        if (tile == null) {
            return;
        }

        // Build the tower
        int x = getTileX(game.getGameMap(), tile);
        int y = getTileY(game.getGameMap(), tile);

        resources.subtract(selectedTower.getCost());
        game.getTowerManager().addTower(new GenericTower(selectedTower, player.getPlayerNumber(), x, y));
        LOG.debug("Bot {} built {} at ({}, {})", player.getPlayerNumber(), selectedTower.getName(), x, y);
    }

    private void tryBuildBuilding(TurrestGameMode02 game, Turrest02Player player, PlayerResources resources) {
        // Pick a random building type we can afford
        List<BuildingDefinition> affordable = new ArrayList<>();
        for (BuildingDefinition def : BuildingDefinition.values()) {
            if (resources.canAfford(def.getCost())) {
                affordable.add(def);
            }
        }

        if (affordable.isEmpty()) {
            return;
        }

        BuildingDefinition selectedBuilding = affordable.get(random.nextInt(affordable.size()));

        // Find a valid tile to build on
        Tile tile = findBuildableTileForBuilding(game, player, selectedBuilding);
        if (tile == null) {
            return;
        }

        int x = getTileX(game.getGameMap(), tile);
        int y = getTileY(game.getGameMap(), tile);

        resources.subtract(selectedBuilding.getCost());
        resources.addProductionBonuses(selectedBuilding.getProductionBonus());
        tile.setStructure(new TurrestBuilding(selectedBuilding, player.getPlayerNumber()));
        LOG.debug("Bot {} built {} at ({}, {})", player.getPlayerNumber(), selectedBuilding.getName(), x, y);
    }

    private void trySendCreep(TurrestGameMode02 game, Turrest02Player player, PlayerResources resources) {
        // Pick a random creep type we can afford
        List<CreepType> affordable = new ArrayList<>();
        for (CreepType type : CreepType.values()) {
            if (type.getSendCost().canAfford(player)) {
                affordable.add(type);
            }
        }

        if (affordable.isEmpty()) {
            return;
        }

        CreepType selectedCreep = affordable.get(random.nextInt(affordable.size()));
        selectedCreep.getSendCost().apply(player);
        game.getCreepManager().spawnSentCreep(selectedCreep, player.getPlayerNumber(), game);
        LOG.debug("Bot {} sent {}", player.getPlayerNumber(), selectedCreep.getId());
    }

    private Tile findBuildableTile(TurrestGameMode02 game, Turrest02Player player, TowerDefinition tower) {
        GameMap map = game.getGameMap();
        List<int[]> candidates = new ArrayList<>();

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null
                        && tile.canPlayerBuild(player.getPlayerNumber())
                        && !tile.hasStructure()
                        && tower.canBuildOn(tile.getTerrainType())) {
                    candidates.add(new int[]{x, y});
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int[] selected = candidates.get(random.nextInt(candidates.size()));
        return map.getTile(selected[0], selected[1]);
    }

    private Tile findBuildableTileForBuilding(TurrestGameMode02 game, Turrest02Player player, BuildingDefinition building) {
        GameMap map = game.getGameMap();
        List<int[]> candidates = new ArrayList<>();

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null
                        && tile.canPlayerBuild(player.getPlayerNumber())
                        && !tile.hasStructure()
                        && building.canBuildOn(tile.getTerrainType())) {
                    candidates.add(new int[]{x, y});
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int[] selected = candidates.get(random.nextInt(candidates.size()));
        return map.getTile(selected[0], selected[1]);
    }

    private int getTileX(GameMap map, Tile tile) {
        // This is a workaround - we should track coordinates differently
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getTile(x, y) == tile) {
                    return x;
                }
            }
        }
        return -1;
    }

    private int getTileY(GameMap map, Tile tile) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getTile(x, y) == tile) {
                    return y;
                }
            }
        }
        return -1;
    }

    @Override
    public String getDifficultyName() {
        return "Easy";
    }
}
