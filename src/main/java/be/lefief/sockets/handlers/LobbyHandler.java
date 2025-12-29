package be.lefief.sockets.handlers;

import be.lefief.game.GameService;
import be.lefief.game.ai.BotSession;
import be.lefief.lobby.Lobby;
import be.lefief.lobby.LobbyPlayer;
import be.lefief.service.lobby.LobbyService;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.AddBotCommand;
import be.lefief.sockets.commands.client.emission.ChangeColorCommand;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import be.lefief.sockets.commands.client.emission.LeaveLobbyCommand;
import be.lefief.sockets.commands.client.emission.RefreshLobbiesCommand;
import be.lefief.sockets.commands.client.emission.RemoveBotCommand;
import be.lefief.sockets.commands.client.emission.RenameLobbyCommand;
import be.lefief.sockets.commands.client.emission.StartLobbyGameCommand;
import be.lefief.sockets.commands.client.emission.ToggleReadyCommand;
import be.lefief.sockets.commands.client.reception.ConnectedToLobbyResponse;
import be.lefief.sockets.commands.client.reception.ErrorMessageResponse;
import be.lefief.sockets.commands.client.reception.GameStartedResponse;
import be.lefief.sockets.commands.client.reception.LobbyCreatedResponse;
import be.lefief.sockets.commands.client.reception.LobbyStateResponse;
import be.lefief.sockets.commands.factories.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void handleGetAllLobbies(SecuredClientToServerCommand<RefreshLobbiesCommand> socketCommand,
            ClientSession clientSession) {
        clientSession.sendCommand(CommandFactory.REFRESH_LOBBIES_RESPONSE(lobbyService.getLobbies()));
    }

    public void handleCreateLobby(SecuredClientToServerCommand<CreateLobbyCommand> socketCommand,
            ClientSession clientSession) {
        CreateLobbyCommand createLobbyCommand = new CreateLobbyCommand(socketCommand.getCommand());
        boolean success = lobbyService.createLobby(socketCommand);
        if (success) {
            clientSession.sendCommand(new LobbyCreatedResponse(socketCommand.getUserId(),
                    createLobbyCommand.getSize(), createLobbyCommand.isHidden(), createLobbyCommand.getPassword(),
                    createLobbyCommand.getGame()));
            // Send lobby state with player list
            Lobby lobby = lobbyService.getLobby(socketCommand.getUserId());
            if (lobby != null) {
                clientSession.sendCommand(new LobbyStateResponse(lobby));
            }
        }
    }

    public void handleJoinLobby(SecuredClientToServerCommand<JoinLobbyCommand> socketCommand,
            ClientSession clientSession) {
        Lobby connectedLobby = lobbyService.joinLobby(socketCommand);
        if (connectedLobby != null) {
            clientSession.sendCommand(new ConnectedToLobbyResponse(connectedLobby.getLobbyID(),
                    connectedLobby.getSize(), connectedLobby.isHidden(), "", connectedLobby.getGame()));
            // Broadcast updated lobby state to all players
            lobbyService.emitLobbyCommand(connectedLobby.getLobbyID(), new LobbyStateResponse(connectedLobby));
        }
    }

    public void handleStartLobbyGame(SecuredClientToServerCommand<StartLobbyGameCommand> socketCommand,
            ClientSession clientSession) {
        Lobby connectedLobby = lobbyService.getLobby(socketCommand.getUserId());
        if (connectedLobby != null) {
            // Check if all players are ready before starting
            if (!connectedLobby.allPlayersReady()) {
                clientSession.sendCommand(new ErrorMessageResponse("All players must be ready to start"));
                return;
            }

            connectedLobby.start();
            lobbyService.emitLobbyMessage(connectedLobby.getLobbyID(),
                    String.format("%s started the game!", clientSession.getUserName()));
            lobbyService.emitLobbyCommand(connectedLobby.getLobbyID(), new GameStartedResponse());

            // Build color map from lobby players (including bots)
            Map<UUID, Integer> playerColorMap = new HashMap<>();
            for (LobbyPlayer lobbyPlayer : connectedLobby.getPlayers()) {
                playerColorMap.put(lobbyPlayer.getId(), lobbyPlayer.getColorIndex());
            }

            // Get human player sessions
            List<ClientSession> playerSessions = lobbyService.getLobbyPlayerSessions(connectedLobby.getLobbyID());

            // Create BotSession instances for bot players
            for (LobbyPlayer lobbyPlayer : connectedLobby.getBots()) {
                BotSession botSession = new BotSession(lobbyPlayer.getId(), lobbyPlayer.getName());
                playerSessions.add(botSession);
            }

            // Start the actual game engine
            gameService.startGame(connectedLobby.getGame(), playerSessions, connectedLobby.getLobbyID(), playerColorMap);
        }
    }

    public void handleChangeColor(SecuredClientToServerCommand<ChangeColorCommand> socketCommand,
            ClientSession clientSession) {
        Lobby lobby = lobbyService.findLobbyByPlayer(socketCommand.getUserId());
        if (lobby == null) {
            clientSession.sendCommand(new ErrorMessageResponse("You are not in a lobby"));
            return;
        }

        int newColorIndex = socketCommand.getCommand().getColorIndex();
        if (newColorIndex < 0 || newColorIndex > 15) {
            clientSession.sendCommand(new ErrorMessageResponse("Invalid color"));
            return;
        }

        boolean success = lobby.changePlayerColor(socketCommand.getUserId(), newColorIndex);
        if (!success) {
            clientSession.sendCommand(new ErrorMessageResponse("Color already taken or you are ready"));
            return;
        }

        // Broadcast updated lobby state to all players
        lobbyService.emitLobbyCommand(lobby.getLobbyID(), new LobbyStateResponse(lobby));
    }

    public void handleToggleReady(SecuredClientToServerCommand<ToggleReadyCommand> socketCommand,
            ClientSession clientSession) {
        Lobby lobby = lobbyService.findLobbyByPlayer(socketCommand.getUserId());
        if (lobby == null) {
            clientSession.sendCommand(new ErrorMessageResponse("You are not in a lobby"));
            return;
        }

        boolean success = lobby.togglePlayerReady(socketCommand.getUserId());
        if (!success) {
            clientSession.sendCommand(new ErrorMessageResponse("Could not toggle ready state"));
            return;
        }

        // Broadcast updated lobby state to all players
        lobbyService.emitLobbyCommand(lobby.getLobbyID(), new LobbyStateResponse(lobby));
    }

    public void handleRenameLobby(SecuredClientToServerCommand<RenameLobbyCommand> socketCommand,
            ClientSession clientSession) {
        Lobby lobby = lobbyService.findLobbyByPlayer(socketCommand.getUserId());
        if (lobby == null) {
            clientSession.sendCommand(new ErrorMessageResponse("You are not in a lobby"));
            return;
        }

        String newName = socketCommand.getCommand().getNewName();
        if (newName == null || newName.trim().isEmpty()) {
            clientSession.sendCommand(new ErrorMessageResponse("Lobby name cannot be empty"));
            return;
        }

        boolean success = lobby.setName(newName, socketCommand.getUserId());
        if (!success) {
            clientSession.sendCommand(new ErrorMessageResponse("Only the host can rename the lobby"));
            return;
        }

        // Broadcast updated lobby state to all players
        lobbyService.emitLobbyCommand(lobby.getLobbyID(), new LobbyStateResponse(lobby));
    }

    public void handleLeaveLobby(SecuredClientToServerCommand<LeaveLobbyCommand> socketCommand,
            ClientSession clientSession) {
        Lobby lobby = lobbyService.findLobbyByPlayer(socketCommand.getUserId());
        if (lobby == null) {
            // Already not in a lobby, no action needed
            return;
        }

        UUID lobbyId = lobby.getLobbyID();
        boolean wasHost = lobby.getLobbyID().equals(socketCommand.getUserId());

        // Remove the player from the lobby
        lobby.removeClient(socketCommand.getUserId());

        // If player was the host and lobby is now empty, remove the lobby
        if (wasHost && lobby.getPlayerIds().isEmpty()) {
            lobbyService.removeLobby(lobbyId);
        } else if (!lobby.getPlayerIds().isEmpty()) {
            // Broadcast updated lobby state to remaining players
            lobbyService.emitLobbyCommand(lobbyId, new LobbyStateResponse(lobby));
        }
    }

    public void handleAddBot(SecuredClientToServerCommand<AddBotCommand> socketCommand,
            ClientSession clientSession) {
        LOG.info("handleAddBot called by user {} ({})", socketCommand.getUserName(), socketCommand.getUserId());

        Lobby lobby = lobbyService.findLobbyByPlayer(socketCommand.getUserId());
        if (lobby == null) {
            LOG.warn("handleAddBot: User {} is not in a lobby", socketCommand.getUserId());
            clientSession.sendCommand(new ErrorMessageResponse("You are not in a lobby"));
            return;
        }

        LOG.info("handleAddBot: Found lobby {} with host {}", lobby.getLobbyID(), lobby.getHost());

        String difficulty = socketCommand.getCommand().getDifficulty();
        boolean success = lobby.addBot(difficulty, socketCommand.getUserId());
        if (!success) {
            LOG.warn("handleAddBot: Failed to add bot. User {} vs Host {}, started={}, players={}/{}",
                    socketCommand.getUserId(), lobby.getHost(), lobby.isStarted(),
                    lobby.getPlayers().size(), lobby.getSize());
            clientSession.sendCommand(new ErrorMessageResponse("Cannot add bot: lobby full or not host"));
            return;
        }

        LOG.info("handleAddBot: Successfully added bot to lobby {}", lobby.getLobbyID());
        // Broadcast updated lobby state to all players
        lobbyService.emitLobbyCommand(lobby.getLobbyID(), new LobbyStateResponse(lobby));
    }

    public void handleRemoveBot(SecuredClientToServerCommand<RemoveBotCommand> socketCommand,
            ClientSession clientSession) {
        Lobby lobby = lobbyService.findLobbyByPlayer(socketCommand.getUserId());
        if (lobby == null) {
            clientSession.sendCommand(new ErrorMessageResponse("You are not in a lobby"));
            return;
        }

        UUID botId = socketCommand.getCommand().getBotId();
        if (botId == null) {
            clientSession.sendCommand(new ErrorMessageResponse("Invalid bot ID"));
            return;
        }

        boolean success = lobby.removeBot(botId, socketCommand.getUserId());
        if (!success) {
            clientSession.sendCommand(new ErrorMessageResponse("Cannot remove bot: not found or not host"));
            return;
        }

        // Broadcast updated lobby state to all players
        lobbyService.emitLobbyCommand(lobby.getLobbyID(), new LobbyStateResponse(lobby));
    }
}
