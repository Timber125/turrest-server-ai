package be.lefief.sockets.handlers;

import be.lefief.game.GameService;
import be.lefief.lobby.Lobby;
import be.lefief.service.lobby.LobbyService;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.ServerSocketSubject;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.sockets.commands.client.emission.RefreshLobbiesCommand;
import be.lefief.sockets.commands.client.emission.StartLobbyGameCommand;
import be.lefief.sockets.commands.client.reception.ConnectedToLobbyResponse;
import be.lefief.sockets.commands.client.reception.GameStartedResponse;
import be.lefief.sockets.commands.client.reception.LobbyCreatedResponse;
import be.lefief.sockets.commands.client.reception.RefreshLobbiesResponse;
import be.lefief.sockets.commands.factories.CommandFactory;
import be.lefief.util.ClientListener;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Ref;
import java.util.List;
import java.util.UUID;

@Service
public class LobbyHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LobbyHandler.class);
    private final LobbyService lobbyService;
    private final GameService gameService;

    public LobbyHandler(LobbyService lobbyService, GameService gameService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
    }

    public void handleGetAllLobbies(SecuredClientToServerCommand<RefreshLobbiesCommand> socketCommand, ClientSession clientSession) {
        RefreshLobbiesCommand refreshLobbiesCommand = new RefreshLobbiesCommand(socketCommand.getCommand());
        clientSession.sendCommand(CommandFactory.REFRESH_LOBBIES_RESPONSE(lobbyService.getLobbies()));
    }

    public void handleCreateLobby(SecuredClientToServerCommand<CreateLobbyCommand> socketCommand, ClientSession clientSession) {
        CreateLobbyCommand createLobbyCommand = new CreateLobbyCommand(socketCommand.getCommand());
        boolean success = lobbyService.createLobby(socketCommand);
        if(success){
            clientSession.sendCommand(new LobbyCreatedResponse(socketCommand.getClientId(), createLobbyCommand.getSize(), createLobbyCommand.isHidden(), createLobbyCommand.getPassword(), createLobbyCommand.getGame())); // TODO
        }
    }

    public void handleJoinLobby(SecuredClientToServerCommand<JoinLobbyCommand> socketCommand, ClientSession clientSession) {
        JoinLobbyCommand joinLobbyCommand = new JoinLobbyCommand(socketCommand.getCommand());
        Lobby connectedLobby = lobbyService.joinLobby(socketCommand);
        if(connectedLobby != null){
            clientSession.sendCommand(new ConnectedToLobbyResponse(connectedLobby.getLobbyID(), connectedLobby.getSize(), connectedLobby.isHidden(), "", connectedLobby.getGame()));
        }
    }

    public void handleStartLobbyGame(SecuredClientToServerCommand<StartLobbyGameCommand> socketCommand, ClientSession clientSession){
        Lobby connectedLobby = lobbyService.getLobby(socketCommand.getClientId());
        if(connectedLobby != null){
            connectedLobby.start();
            lobbyService.emitLobbyMessage(connectedLobby.getLobbyID(), String.format("%s started the game!", clientSession.getClientName()));
            lobbyService.emitLobbyCommand(connectedLobby.getLobbyID(), new GameStartedResponse());

            // Start the actual game engine
            List<ClientSession> playerSessions = lobbyService.getLobbyPlayerSessions(connectedLobby.getLobbyID());
            gameService.startGame(connectedLobby.getGame(), playerSessions);
        }
    }


}
