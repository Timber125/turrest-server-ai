package be.lefief.game.turrest01.event;

/**
 * Fired when a creep is killed by towers.
 */
public class CreepKilledEvent extends TurrestEvent {
    private final String creepTypeId;
    private final int goldReward;

    public CreepKilledEvent(int playerNumber, String creepTypeId, int goldReward) {
        super(playerNumber);
        this.creepTypeId = creepTypeId;
        this.goldReward = goldReward;
    }

    public String getCreepTypeId() {
        return creepTypeId;
    }

    public int getGoldReward() {
        return goldReward;
    }
}
