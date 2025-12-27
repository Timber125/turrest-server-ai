package be.lefief.game.turrest01.commands;

import be.lefief.game.map.GameMap;
import be.lefief.game.map.Structure;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.structure.TurrestBuilding;
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

        return data;
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
