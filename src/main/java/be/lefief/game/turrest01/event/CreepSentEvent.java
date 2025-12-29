package be.lefief.game.turrest01.event;

/**
 * Fired when a player sends a creep to opponents.
 */
public class CreepSentEvent extends TurrestEvent {
    private final String creepTypeId;
    private final int goldCost;

    public CreepSentEvent(int playerNumber, String creepTypeId, int goldCost) {
        super(playerNumber);
        this.creepTypeId = creepTypeId;
        this.goldCost = goldCost;
    }

    public String getCreepTypeId() {
        return creepTypeId;
    }

    public int getGoldCost() {
        return goldCost;
    }
}
