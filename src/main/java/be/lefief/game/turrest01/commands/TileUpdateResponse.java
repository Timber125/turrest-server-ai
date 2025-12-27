package be.lefief.game.turrest01.commands;

import be.lefief.game.map.Structure;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.structure.TurrestBuilding;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends tile update information including terrain and structure data.
 */
public class TileUpdateResponse extends ServerToClientCommand {

    public static final String TOPIC = "TILE_UPDATE";

    public TileUpdateResponse(int x, int y, Tile tile) {
        super(ClientSocketSubject.GAME, TOPIC, createData(x, y, tile));
    }

    private static Map<String, Object> createData(int x, int y, Tile tile) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("terrainType", tile.getTerrainType().getTerrainTypeID());

        Structure structure = tile.getStructure();
        if (structure != null) {
            data.put("structureType", structure.getStructureTypeId());

            if (structure instanceof TurrestBuilding building) {
                data.put("buildingType", building.getBuildingTypeId());
                data.put("ownerPlayerNumber", building.getOwnerPlayerNumber());
            }
        }

        // Add tile ownership data
        data.put("owners", new ArrayList<>(tile.getOwners()));

        return data;
    }
}
