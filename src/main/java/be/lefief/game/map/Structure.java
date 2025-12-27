package be.lefief.game.map;

/**
 * Base class for all structures that can be placed on tiles.
 * Game-specific structures should extend this class.
 */
public abstract class Structure {

    /**
     * Returns a unique identifier for the structure type.
     * Used for serialization and client communication.
     */
    public abstract int getStructureTypeId();
}
