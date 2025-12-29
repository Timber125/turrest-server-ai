package be.lefief.game.turrest02.tower;

/**
 * Generic tower implementation that works with any TowerDefinition.
 * Used for all tower types except those needing custom behavior.
 */
public class GenericTower extends Tower {

    private final TowerDefinition definition;

    public GenericTower(TowerDefinition definition, int ownerPlayerNumber, int tileX, int tileY) {
        super(ownerPlayerNumber, tileX, tileY);
        this.definition = definition;
    }

    @Override
    public TowerDefinition getDefinition() {
        return definition;
    }
}
