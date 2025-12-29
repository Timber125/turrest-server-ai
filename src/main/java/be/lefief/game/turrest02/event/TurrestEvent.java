package be.lefief.game.turrest02.event;

/**
 * Base class for all Turrest game events.
 * Events are used for statistics tracking.
 */
public abstract class TurrestEvent {
    protected final long timestamp;
    protected final int playerNumber;

    protected TurrestEvent(int playerNumber) {
        this.timestamp = System.currentTimeMillis();
        this.playerNumber = playerNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
}
