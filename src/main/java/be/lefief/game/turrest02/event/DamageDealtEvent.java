package be.lefief.game.turrest02.event;

/**
 * Fired when damage is dealt to a creep.
 */
public class DamageDealtEvent extends TurrestEvent {
    private final int damage;
    private final int towerTypeId;

    public DamageDealtEvent(int playerNumber, int damage, int towerTypeId) {
        super(playerNumber);
        this.damage = damage;
        this.towerTypeId = towerTypeId;
    }

    public int getDamage() {
        return damage;
    }

    public int getTowerTypeId() {
        return towerTypeId;
    }
}
