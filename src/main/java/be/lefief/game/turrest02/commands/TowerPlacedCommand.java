package be.lefief.game.turrest02.commands;

import be.lefief.game.turrest02.tower.Tower;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Command sent when a tower is placed on the map.
 */
public class TowerPlacedCommand extends ServerToClientCommand {

    public static final String TOPIC = "TOWER_PLACED";

    public TowerPlacedCommand(Tower tower, int tickRateMs) {
        super(ClientSocketSubject.GAME, TOPIC, createData(tower, tickRateMs));
    }

    private static Map<String, Object> createData(Tower tower, int tickRateMs) {
        Map<String, Object> data = new HashMap<>();
        data.put("towerId", tower.getId().toString());
        data.put("towerType", tower.getDefinition().getId());
        data.put("towerName", tower.getDefinition().getName());
        data.put("x", tower.getTileX());
        data.put("y", tower.getTileY());
        data.put("playerNumber", tower.getOwnerPlayerNumber());
        data.put("range", tower.getShootingRange());
        data.put("damage", tower.getBulletDamage());
        data.put("cooldownMs", tower.getCooldownMs());
        data.put("theoreticalFireRate", tower.getTheoreticalFireRate());
        data.put("practicalFireRate", tower.getPracticalFireRate(tickRateMs));
        return data;
    }
}
