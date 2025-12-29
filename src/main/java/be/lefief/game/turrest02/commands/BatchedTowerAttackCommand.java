package be.lefief.game.turrest02.commands;

import be.lefief.game.turrest02.tower.TowerAttack;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batched tower attack command - sends all tower attacks in a single command
 * to reduce network overhead.
 */
public class BatchedTowerAttackCommand extends ServerToClientCommand {

    public static final String TOPIC = "BATCHED_TOWER_ATTACK";

    public BatchedTowerAttackCommand(List<TowerAttack> attacks) {
        super(ClientSocketSubject.GAME, TOPIC, createData(attacks));
    }

    private static Map<String, Object> createData(List<TowerAttack> attacks) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> attackList = new ArrayList<>();

        for (TowerAttack attack : attacks) {
            Map<String, Object> attackData = new HashMap<>();
            attackData.put("towerId", attack.getTower().getId().toString());
            attackData.put("towerX", attack.getTowerX());
            attackData.put("towerY", attack.getTowerY());
            attackData.put("targetCreepId", attack.getTarget().getId().toString());
            attackData.put("targetX", attack.getTargetX());
            attackData.put("targetY", attack.getTargetY());
            attackData.put("damage", attack.getDamage());
            attackData.put("bulletType", attack.getBulletType());
            attackList.add(attackData);
        }

        data.put("attacks", attackList);
        return data;
    }
}
