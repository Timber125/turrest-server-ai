package be.lefief.game.turrest01.commands;

import be.lefief.game.turrest01.creep.Creep;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class DespawnCreepCommand extends ServerToClientCommand {

    public static final String TOPIC = "DESPAWN_CREEP";

    public DespawnCreepCommand(Creep creep) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creep, 0, -1));
    }

    public DespawnCreepCommand(Creep creep, int goldAwarded, int awardedToPlayer) {
        super(ClientSocketSubject.GAME, TOPIC, createData(creep, goldAwarded, awardedToPlayer));
    }

    private static Map<String, Object> createData(Creep creep, int goldAwarded, int awardedToPlayer) {
        Map<String, Object> data = new HashMap<>();
        data.put("creepId", creep.getId().toString());
        data.put("x", creep.getX());
        data.put("y", creep.getY());
        data.put("goldAwarded", goldAwarded);
        data.put("awardedToPlayer", awardedToPlayer);
        return data;
    }
}
