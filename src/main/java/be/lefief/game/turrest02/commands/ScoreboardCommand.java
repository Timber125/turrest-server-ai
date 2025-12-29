package be.lefief.game.turrest02.commands;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScoreboardCommand extends ServerToClientCommand {

    public static final String TOPIC = "SCOREBOARD";

    public ScoreboardCommand(List<PlayerScoreEntry> entries) {
        super(ClientSocketSubject.GAME, TOPIC, createData(entries));
    }

    private static Map<String, Object> createData(List<PlayerScoreEntry> entries) {
        Map<String, Object> data = new HashMap<>();
        data.put("players", entries.stream().map(e -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("playerNumber", e.getPlayerNumber());
            entry.put("colorIndex", e.getColorIndex());
            entry.put("username", e.getUsername());
            entry.put("score", e.getScore());
            entry.put("isAlive", e.isAlive());
            return entry;
        }).collect(Collectors.toList()));
        return data;
    }
}
