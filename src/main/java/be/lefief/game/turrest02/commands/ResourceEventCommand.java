package be.lefief.game.turrest02.commands;

import be.lefief.game.turrest02.resource.ResourceCost;
import be.lefief.game.turrest02.resource.ResourceEventType;
import be.lefief.game.turrest02.resource.TurrestCost;
import be.lefief.game.turrest02.resource.TurrestReward;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to notify frontend of resource changes for animation purposes.
 * Used for both rewards (positive values) and costs (negative values).
 */
public class ResourceEventCommand extends ServerToClientCommand {

    public static final String TOPIC = "RESOURCE_EVENT";

    /**
     * Create a resource event from a reward (positive values).
     */
    public ResourceEventCommand(ResourceEventType eventType, TurrestReward reward,
                                double x, double y, int playerNumber) {
        super(ClientSocketSubject.GAME, TOPIC, createData(
                eventType,
                reward.getWood(),
                reward.getStone(),
                reward.getGold(),
                reward.getHitpoints(),
                x, y, playerNumber));
    }

    /**
     * Create a resource event from a TurrestCost (will be shown as negative values).
     */
    public ResourceEventCommand(ResourceEventType eventType, TurrestCost cost,
                                double x, double y, int playerNumber) {
        super(ClientSocketSubject.GAME, TOPIC, createData(
                eventType,
                -cost.getWood(),
                -cost.getStone(),
                -cost.getGold(),
                -cost.getHitpoints(),
                x, y, playerNumber));
    }

    /**
     * Create a resource event from a ResourceCost (will be shown as negative values, no hitpoints).
     */
    public ResourceEventCommand(ResourceEventType eventType, ResourceCost cost,
                                double x, double y, int playerNumber) {
        super(ClientSocketSubject.GAME, TOPIC, createData(
                eventType,
                -cost.getWood(),
                -cost.getStone(),
                -cost.getGold(),
                0,  // ResourceCost has no hitpoints
                x, y, playerNumber));
    }

    private static Map<String, Object> createData(ResourceEventType eventType,
                                                  int wood, int stone, int gold, int hitpoints,
                                                  double x, double y, int playerNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", eventType.getId());
        data.put("wood", wood);
        data.put("stone", stone);
        data.put("gold", gold);
        data.put("hitpoints", hitpoints);
        data.put("x", x);
        data.put("y", y);
        data.put("playerNumber", playerNumber);
        return data;
    }
}
