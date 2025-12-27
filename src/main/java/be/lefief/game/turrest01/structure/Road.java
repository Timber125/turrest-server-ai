package be.lefief.game.turrest01.structure;

import be.lefief.game.map.Structure;

/**
 * Represents a road tile in Turrest01.
 * Roads connect spawners to castles and prevent building placement.
 */
public class Road extends Structure {

    @Override
    public int getStructureTypeId() {
        return TurrestStructureType.ROAD.getId();
    }
}
