package be.lefief.game.turrest01.event;

/**
 * Fired when a building is constructed.
 */
public class BuildingBuiltEvent extends TurrestEvent {
    private final int buildingTypeId;
    private final int x;
    private final int y;

    public BuildingBuiltEvent(int playerNumber, int buildingTypeId, int x, int y) {
        super(playerNumber);
        this.buildingTypeId = buildingTypeId;
        this.x = x;
        this.y = y;
    }

    public int getBuildingTypeId() {
        return buildingTypeId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
