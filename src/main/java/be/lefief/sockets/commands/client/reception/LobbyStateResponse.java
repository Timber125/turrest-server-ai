package be.lefief.sockets.commands.client.reception;

import be.lefief.lobby.Lobby;
import be.lefief.lobby.LobbyPlayer;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyStateResponse extends ServerToClientCommand {

    public static final String TOPIC = "LOBBY_STATE";

    public LobbyStateResponse(Lobby lobby) {
        super(ClientSocketSubject.LOBBY, TOPIC, createData(lobby));
    }

    private static Map<String, Object> createData(Lobby lobby) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobbyId", lobby.getLobbyID().toString());
        data.put("hostId", lobby.getHost().toString());
        data.put("name", lobby.getName());
        data.put("game", lobby.getGame());
        data.put("size", lobby.getSize());
        data.put("hidden", lobby.isHidden());
        data.put("allReady", lobby.allPlayersReady());

        List<Map<String, Object>> players = new ArrayList<>();
        for (LobbyPlayer player : lobby.getPlayers()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("id", player.getId().toString());
            playerData.put("name", player.getName());
            playerData.put("colorIndex", player.getColorIndex());
            playerData.put("ready", player.isReady());
            players.add(playerData);
        }
        data.put("players", players);

        return data;
    }
}
