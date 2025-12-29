package be.lefief.sockets.commands.factories;

import be.lefief.lobby.Lobby;
import be.lefief.sockets.commands.client.ClientSocketSubject;
import be.lefief.sockets.commands.client.reception.DisplayChatCommand;
import be.lefief.sockets.commands.client.reception.RefreshLobbiesResponse;
import be.lefief.util.DateUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static be.lefief.sockets.commands.client.reception.RefreshLobbiesResponse.*;

public class CommandFactory {
    public static DisplayChatCommand USER_MESSAGE(LocalDateTime timeSent, String userName, UUID userId, String message){
        return new DisplayChatCommand(String.format("%s - %s(%s): %s", DateUtil.SHORT_TIMEFORMAT.format(timeSent), userName, "#" + userId.toString().substring(0, 4), message));
    }

    public static DisplayChatCommand TIMED_SERVER_MESSAGE(LocalDateTime timeSent, String message){
        return new DisplayChatCommand(String.format("%s - SERVER: %s", DateUtil.SHORT_TIMEFORMAT.format(timeSent), message));
    }
    public static DisplayChatCommand SERVER_MESSAGE(String message){
        return new DisplayChatCommand(String.format("SERVER: %s", message));
    }

    public static RefreshLobbiesResponse REFRESH_LOBBIES_RESPONSE(List<Lobby> lobbies) {
        return new RefreshLobbiesResponse(new HashMap<>() {{
            put(NUMBER_OF_LOBBIES, lobbies.size());
            for(int i = 0; i < lobbies.size(); i++){
                Lobby lobby_i = lobbies.get(i);
                put(LOBBY_ID(i), lobby_i.getLobbyID());
                put(LOBBY_SIZE(i), lobby_i.getSize());
                put(HIDDEN(i), lobby_i.isHidden());
                put(PASSWORD(i), null);
                put(GAME(i), lobby_i.getGame());
                put(NAME(i), lobby_i.getName());
            }
        }});
    }

}
