package be.lefief.game.turrest02.tower;

import be.lefief.game.turrest02.creep.Creep;
import lombok.Getter;

/**
 * Represents a single tower attack for broadcasting to clients.
 */
@Getter
public class TowerAttack {

    private final Tower tower;
    private final Creep target;
    private final double towerX;
    private final double towerY;
    private final double targetX;
    private final double targetY;
    private final int damage;
    private final String bulletType;

    public TowerAttack(Tower tower, Creep target) {
        this.tower = tower;
        this.target = target;
        this.towerX = tower.getCenterX();
        this.towerY = tower.getCenterY();
        this.targetX = target.getX();
        this.targetY = target.getY();
        this.damage = tower.getBulletDamage();
        this.bulletType = tower.getBulletType();
    }
}
