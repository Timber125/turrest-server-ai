package be.lefief.game.turrest01.building;

import be.lefief.game.map.TerrainType;
import be.lefief.game.turrest01.resource.ResourceCost;
import be.lefief.game.turrest01.resource.ResourceType;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public enum BuildingDefinition {
    LUMBERCAMP(1, "Lumbercamp",
            new ResourceCost(50, 10, 10),
            List.of(TerrainType.FOREST),
            Map.of(ResourceType.WOOD, 1)),

    STONE_QUARRY(2, "Stone Quarry",
            new ResourceCost(10, 50, 10),
            List.of(TerrainType.ROCKY),
            Map.of(ResourceType.STONE, 1)),

    GOLD_MINE(3, "Gold Mine",
            new ResourceCost(10, 10, 50),
            List.of(TerrainType.DIRT),
            Map.of(ResourceType.GOLD, 1));

    private final int id;
    private final String name;
    private final ResourceCost cost;
    private final List<TerrainType> allowedTerrains;
    private final Map<ResourceType, Integer> productionBonus;

    BuildingDefinition(int id, String name, ResourceCost cost,
                       List<TerrainType> allowedTerrains,
                       Map<ResourceType, Integer> productionBonus) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.allowedTerrains = allowedTerrains;
        this.productionBonus = productionBonus;
    }

    /**
     * Check if this building can be built on the given terrain type.
     */
    public boolean canBuildOn(TerrainType terrain) {
        return allowedTerrains.contains(terrain);
    }

    /**
     * Get a BuildingDefinition by its ID.
     */
    public static BuildingDefinition fromId(int id) {
        for (BuildingDefinition def : values()) {
            if (def.id == id) {
                return def;
            }
        }
        return null;
    }
}
