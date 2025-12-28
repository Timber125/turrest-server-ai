package be.lefief.game.turrest01.tower;

/**
 * Basic tower - the starting defensive tower.
 * Range: 3 tiles, Cooldown: 1000ms (1 shot/sec), Damage: 10
 */
public class BasicTower extends Tower {

    public BasicTower(int ownerPlayerNumber, int tileX, int tileY) {
        super(ownerPlayerNumber, tileX, tileY);
    }

    @Override
    public TowerDefinition getDefinition() {
        return TowerDefinition.BASIC_TOWER;
    }
}
