package be.lefief.game.turrest01.structure;

import be.lefief.game.map.Structure;
import be.lefief.game.turrest01.building.BuildingDefinition;
import lombok.Getter;

/**
 * Represents a player-constructed building in Turrest01.
 */
@Getter
public class TurrestBuilding extends Structure {

    private final BuildingDefinition definition;
    private final int ownerPlayerNumber;

    public TurrestBuilding(BuildingDefinition definition, int ownerPlayerNumber) {
        this.definition = definition;
        this.ownerPlayerNumber = ownerPlayerNumber;
    }

    @Override
    public int getStructureTypeId() {
        return TurrestStructureType.BUILDING.getId();
    }

    public int getBuildingTypeId() {
        return definition.getId();
    }
}
