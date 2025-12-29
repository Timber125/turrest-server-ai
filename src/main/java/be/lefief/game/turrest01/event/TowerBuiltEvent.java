package be.lefief.game.turrest01.event;

/**
 * Fired when a tower is constructed.
 */
public class TowerBuiltEvent extends TurrestEvent {
    private final int towerTypeId;
    private final int x;
    private final int y;

    public TowerBuiltEvent(int playerNumber, int towerTypeId, int x, int y) {
        super(playerNumber);
        this.towerTypeId = towerTypeId;
        this.x = x;
        this.y = y;
    }

    public int getTowerTypeId() {
        return towerTypeId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
