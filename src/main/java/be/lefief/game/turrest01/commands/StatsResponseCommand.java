package be.lefief.game.turrest01.commands;

import be.lefief.game.turrest01.stats.GameStats;
import be.lefief.game.turrest01.stats.PlayerStats;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response containing game statistics.
 */
public class StatsResponseCommand extends ServerToClientCommand {

    public static final String TOPIC = "STATS";

    public StatsResponseCommand(GameStats stats) {
        super(ClientSocketSubject.GAME, TOPIC, createData(stats));
    }

    private static Map<String, Object> createData(GameStats stats) {
        Map<String, Object> data = new HashMap<>();
        data.put("gameDurationMs", stats.getGameDurationMs());

        List<Map<String, Object>> playerStatsList = new ArrayList<>();
        for (PlayerStats ps : stats.getAllPlayerStats().values()) {
            playerStatsList.add(ps.toMap());
        }
        data.put("players", playerStatsList);

        return data;
    }
}
