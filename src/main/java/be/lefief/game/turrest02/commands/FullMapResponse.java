package be.lefief.game.turrest02.commands;

import be.lefief.game.map.GameMap;
import be.lefief.game.map.Structure;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest02.creep.CreepType;
import be.lefief.game.turrest02.structure.TurrestBuilding;
import be.lefief.game.turrest02.tower.TowerDefinition;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the entire game map in a single command for efficient initial load.
 */
public class FullMapResponse extends ServerToClientCommand {

    public static final String TOPIC = "FULL_MAP";

    public FullMapResponse(GameMap gameMap, Map<Integer, Integer> playerColorMap) {
        super(ClientSocketSubject.GAME, TOPIC, createData(gameMap, playerColorMap));
    }

    private static Map<String, Object> createData(GameMap gameMap, Map<Integer, Integer> playerColorMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("width", gameMap.getWidth());
        data.put("height", gameMap.getHeight());
        data.put("playerColorMap", playerColorMap);

        List<Map<String, Object>> tiles = new ArrayList<>();
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    tiles.add(createTileData(x, y, tile));
                }
            }
        }
        data.put("tiles", tiles);

        // Add available tower definitions
        data.put("towers", createTowerDefinitions());

        // Add available creep definitions
        data.put("creeps", createCreepDefinitions());

        return data;
    }

    private static List<Map<String, Object>> createTowerDefinitions() {
        List<Map<String, Object>> towers = new ArrayList<>();
        for (TowerDefinition def : TowerDefinition.values()) {
            Map<String, Object> tower = new HashMap<>();
            tower.put("id", def.getId());
            tower.put("name", def.getName());
            tower.put("range", def.getShootingRange());
            tower.put("damage", def.getBulletDamage());
            tower.put("cooldownMs", def.getCooldownMs());
            tower.put("bulletType", def.getBulletType());
            tower.put("costWood", def.getCost().getWood());
            tower.put("costStone", def.getCost().getStone());
            tower.put("costGold", def.getCost().getGold());
            tower.put("splashRadius", def.getSplashRadius());
            tower.put("slowFactor", def.getSlowFactor());
            tower.put("slowDurationMs", def.getSlowDurationMs());
            towers.add(tower);
        }
        return towers;
    }

    private static List<Map<String, Object>> createCreepDefinitions() {
        List<Map<String, Object>> creeps = new ArrayList<>();
        for (CreepType type : CreepType.values()) {
            Map<String, Object> creep = new HashMap<>();
            creep.put("id", type.getId());
            creep.put("speed", type.getSpeed());
            creep.put("hitpoints", type.getHitpoints());
            creep.put("damage", type.getDamage());
            creep.put("sendCostGold", type.getSendCost().getGold());
            creep.put("killRewardGold", type.getKillReward().getGold());
            creep.put("spawnCount", type.getSpawnCount());
            creep.put("isHealer", type.canHeal());
            creeps.add(creep);
        }
        return creeps;
    }

    private static Map<String, Object> createTileData(int x, int y, Tile tile) {
        Map<String, Object> tileData = new HashMap<>();
        tileData.put("x", x);
        tileData.put("y", y);
        tileData.put("terrainType", tile.getTerrainType().getTerrainTypeID());

        Structure structure = tile.getStructure();
        if (structure != null) {
            tileData.put("structureType", structure.getStructureTypeId());

            if (structure instanceof TurrestBuilding building) {
                tileData.put("buildingType", building.getBuildingTypeId());
                tileData.put("ownerPlayerNumber", building.getOwnerPlayerNumber());
            }
        }

        tileData.put("owners", new ArrayList<>(tile.getOwners()));

        return tileData;
    }
}
