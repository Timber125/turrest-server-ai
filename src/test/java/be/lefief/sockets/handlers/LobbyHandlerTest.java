package be.lefief.sockets.handlers;

import be.lefief.game.GameService;
import be.lefief.lobby.Lobby;
import be.lefief.lobby.LobbyPlayer;
import be.lefief.service.lobby.LobbyService;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.emission.*;
import be.lefief.sockets.commands.client.reception.ErrorMessageResponse;
import be.lefief.sockets.commands.client.reception.LobbyStateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LobbyHandler Tests")
class LobbyHandlerTest {

    @Mock
    private LobbyService lobbyService;

    @Mock
    private GameService gameService;

    @Mock
    private ClientSession clientSession;

    private LobbyHandler lobbyHandler;
    private UUID hostId;
    private String hostName;

    @BeforeEach
    void setUp() {
        lobbyHandler = new LobbyHandler(lobbyService, gameService);
        hostId = UUID.randomUUID();
        hostName = "TestHost";
    }

    private Lobby createTestLobby() {
        return new Lobby(hostId, hostName, 4, false, "test-game");
    }

    @Test
    @DisplayName("Handle create lobby sends confirmation and state")
    void testHandleCreateLobby_sendsConfirmation() {
        Map<String, Object> data = new HashMap<>();
        data.put("size", 4);
        data.put("hidden", false);
        data.put("password", null);
        data.put("game", "test-game");
        data.put("name", null);
        CreateLobbyCommand cmd = new CreateLobbyCommand(data);
        SecuredClientToServerCommand<CreateLobbyCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.createLobby(any())).thenReturn(true);
        when(lobbyService.getLobby(hostId)).thenReturn(lobby);

        lobbyHandler.handleCreateLobby(command, clientSession);

        // Verify two commands are sent: LobbyCreatedResponse and LobbyStateResponse
        verify(clientSession, times(2)).sendCommand(any(ServerToClientCommand.class));
    }

    @Test
    @DisplayName("Handle join lobby broadcasts state to all players")
    void testHandleJoinLobby_broadcastsState() {
        UUID playerId = UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("lobby_ID", hostId.toString());
        JoinLobbyCommand cmd = new JoinLobbyCommand(data);
        SecuredClientToServerCommand<JoinLobbyCommand> command =
                new SecuredClientToServerCommand<>(cmd, playerId, "Player2");

        Lobby lobby = createTestLobby();
        lobby.addClient(playerId, "Player2");
        when(lobbyService.joinLobby(any())).thenReturn(lobby);

        lobbyHandler.handleJoinLobby(command, clientSession);

        // Verify connected response sent to joining player
        verify(clientSession).sendCommand(any(ServerToClientCommand.class));
        // Verify lobby state broadcast
        verify(lobbyService).emitLobbyCommand(eq(lobby.getLobbyID()), any(LobbyStateResponse.class));
    }

    @Test
    @DisplayName("Handle change color broadcasts updated state")
    void testHandleChangeColor_broadcastsState() {
        Map<String, Object> data = new HashMap<>();
        data.put("colorIndex", 5);
        ChangeColorCommand cmd = new ChangeColorCommand(data);
        SecuredClientToServerCommand<ChangeColorCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(lobby);

        lobbyHandler.handleChangeColor(command, clientSession);

        verify(lobbyService).emitLobbyCommand(eq(lobby.getLobbyID()), any(LobbyStateResponse.class));
        assertEquals(5, lobby.getPlayer(hostId).getColorIndex());
    }

    @Test
    @DisplayName("Handle change color sends error when not in lobby")
    void testHandleChangeColor_notInLobby() {
        Map<String, Object> data = new HashMap<>();
        data.put("colorIndex", 5);
        ChangeColorCommand cmd = new ChangeColorCommand(data);
        SecuredClientToServerCommand<ChangeColorCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(null);

        lobbyHandler.handleChangeColor(command, clientSession);

        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle change color sends error for invalid color")
    void testHandleChangeColor_invalidColor() {
        Map<String, Object> data = new HashMap<>();
        data.put("colorIndex", 20); // Invalid color
        ChangeColorCommand cmd = new ChangeColorCommand(data);
        SecuredClientToServerCommand<ChangeColorCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(lobby);

        lobbyHandler.handleChangeColor(command, clientSession);

        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle toggle ready broadcasts updated state")
    void testHandleToggleReady_broadcastsState() {
        Map<String, Object> data = new HashMap<>();
        ToggleReadyCommand cmd = new ToggleReadyCommand(data);
        SecuredClientToServerCommand<ToggleReadyCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(lobby);

        assertFalse(lobby.getPlayer(hostId).isReady());

        lobbyHandler.handleToggleReady(command, clientSession);

        verify(lobbyService).emitLobbyCommand(eq(lobby.getLobbyID()), any(LobbyStateResponse.class));
        assertTrue(lobby.getPlayer(hostId).isReady());
    }

    @Test
    @DisplayName("Handle toggle ready sends error when not in lobby")
    void testHandleToggleReady_notInLobby() {
        Map<String, Object> data = new HashMap<>();
        ToggleReadyCommand cmd = new ToggleReadyCommand(data);
        SecuredClientToServerCommand<ToggleReadyCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(null);

        lobbyHandler.handleToggleReady(command, clientSession);

        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle start game when all players ready")
    void testHandleStartLobbyGame_allReady() {
        Map<String, Object> data = new HashMap<>();
        StartLobbyGameCommand cmd = new StartLobbyGameCommand(data);
        SecuredClientToServerCommand<StartLobbyGameCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        lobby.getPlayer(hostId).setReady(true);
        when(lobbyService.getLobby(hostId)).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerSessions(any())).thenReturn(new ArrayList<>());
        when(clientSession.getUserName()).thenReturn(hostName);

        lobbyHandler.handleStartLobbyGame(command, clientSession);

        assertTrue(lobby.isStarted());
        verify(gameService).startGame(eq("test-game"), any(), any(), any());
    }

    @Test
    @DisplayName("Handle start game fails when not all players ready")
    void testHandleStartLobbyGame_notAllReady() {
        Map<String, Object> data = new HashMap<>();
        StartLobbyGameCommand cmd = new StartLobbyGameCommand(data);
        SecuredClientToServerCommand<StartLobbyGameCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        UUID player2 = UUID.randomUUID();
        lobby.addClient(player2, "Player2");
        lobby.getPlayer(hostId).setReady(true);
        // Player2 not ready
        when(lobbyService.getLobby(hostId)).thenReturn(lobby);

        lobbyHandler.handleStartLobbyGame(command, clientSession);

        assertFalse(lobby.isStarted());
        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle rename lobby broadcasts updated state")
    void testHandleRenameLobby_success() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "New Lobby Name");
        RenameLobbyCommand cmd = new RenameLobbyCommand(data);
        SecuredClientToServerCommand<RenameLobbyCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(lobby);

        lobbyHandler.handleRenameLobby(command, clientSession);

        assertEquals("New Lobby Name", lobby.getName());
        verify(lobbyService).emitLobbyCommand(eq(lobby.getLobbyID()), any(LobbyStateResponse.class));
    }

    @Test
    @DisplayName("Handle rename lobby sends error when not host")
    void testHandleRenameLobby_notHost() {
        UUID playerId = UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "New Lobby Name");
        RenameLobbyCommand cmd = new RenameLobbyCommand(data);
        SecuredClientToServerCommand<RenameLobbyCommand> command =
                new SecuredClientToServerCommand<>(cmd, playerId, "Player2");

        Lobby lobby = createTestLobby();
        lobby.addClient(playerId, "Player2");
        when(lobbyService.findLobbyByPlayer(playerId)).thenReturn(lobby);

        lobbyHandler.handleRenameLobby(command, clientSession);

        assertEquals(hostName + "'s lobby", lobby.getName()); // Name unchanged
        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle rename lobby sends error for empty name")
    void testHandleRenameLobby_emptyName() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "");
        RenameLobbyCommand cmd = new RenameLobbyCommand(data);
        SecuredClientToServerCommand<RenameLobbyCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(lobby);

        lobbyHandler.handleRenameLobby(command, clientSession);

        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle rename lobby sends error when not in lobby")
    void testHandleRenameLobby_notInLobby() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "New Name");
        RenameLobbyCommand cmd = new RenameLobbyCommand(data);
        SecuredClientToServerCommand<RenameLobbyCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(null);

        lobbyHandler.handleRenameLobby(command, clientSession);

        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle add bot successfully adds bot to lobby")
    void testHandleAddBot_success() {
        Map<String, Object> data = new HashMap<>();
        data.put("difficulty", "EASY");
        AddBotCommand cmd = new AddBotCommand(data);
        SecuredClientToServerCommand<AddBotCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        Lobby lobby = createTestLobby();
        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(lobby);

        lobbyHandler.handleAddBot(command, clientSession);

        // Verify bot was added
        assertEquals(2, lobby.getPlayers().size());
        assertTrue(lobby.getPlayers().stream().anyMatch(p -> p.isBot()));
        // Verify lobby state broadcast
        verify(lobbyService).emitLobbyCommand(eq(lobby.getLobbyID()), any(LobbyStateResponse.class));
    }

    @Test
    @DisplayName("Handle add bot sends error when not in lobby")
    void testHandleAddBot_notInLobby() {
        Map<String, Object> data = new HashMap<>();
        data.put("difficulty", "EASY");
        AddBotCommand cmd = new AddBotCommand(data);
        SecuredClientToServerCommand<AddBotCommand> command =
                new SecuredClientToServerCommand<>(cmd, hostId, hostName);

        when(lobbyService.findLobbyByPlayer(hostId)).thenReturn(null);

        lobbyHandler.handleAddBot(command, clientSession);

        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }

    @Test
    @DisplayName("Handle add bot sends error when not host")
    void testHandleAddBot_notHost() {
        UUID playerId = UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("difficulty", "EASY");
        AddBotCommand cmd = new AddBotCommand(data);
        SecuredClientToServerCommand<AddBotCommand> command =
                new SecuredClientToServerCommand<>(cmd, playerId, "Player2");

        Lobby lobby = createTestLobby();
        lobby.addClient(playerId, "Player2");
        when(lobbyService.findLobbyByPlayer(playerId)).thenReturn(lobby);

        lobbyHandler.handleAddBot(command, clientSession);

        // Bot should not be added (only 2 players: host + Player2)
        assertEquals(2, lobby.getPlayers().size());
        assertFalse(lobby.getPlayers().stream().skip(1).anyMatch(p -> p.isBot()));
        // Error should be sent
        ArgumentCaptor<ServerToClientCommand> captor = ArgumentCaptor.forClass(ServerToClientCommand.class);
        verify(clientSession).sendCommand(captor.capture());
        assertTrue(captor.getValue() instanceof ErrorMessageResponse);
    }
}
