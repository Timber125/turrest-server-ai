package be.lefief.service.lobby;

import be.lefief.lobby.Lobby;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.sockets.commands.client.emission.JoinLobbyCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LobbyService Tests")
class LobbyServiceTest {

    private LobbyService lobbyService;

    @Mock
    private ClientSession mockClientSession;

    private UUID hostId;
    private String hostName;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService();
        hostId = UUID.randomUUID();
        hostName = "TestHost";
    }

    private SecuredClientToServerCommand<CreateLobbyCommand> createLobbyCommand(
            UUID userId, String userName, int size, boolean hidden, String game, String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("size", size);
        data.put("hidden", hidden);
        data.put("password", null);
        data.put("game", game);
        data.put("name", name);
        CreateLobbyCommand cmd = new CreateLobbyCommand(data);
        return new SecuredClientToServerCommand<>(cmd, userId, userName);
    }

    private SecuredClientToServerCommand<JoinLobbyCommand> joinLobbyCommand(
            UUID userId, String userName, UUID lobbyId) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobby_ID", lobbyId.toString());
        JoinLobbyCommand cmd = new JoinLobbyCommand(data);
        return new SecuredClientToServerCommand<>(cmd, userId, userName);
    }

    @Test
    @DisplayName("Create lobby successfully")
    void testCreateLobby_success() {
        var command = createLobbyCommand(hostId, hostName, 4, false, "test-game", null);

        boolean result = lobbyService.createLobby(command);

        assertTrue(result);
        Lobby lobby = lobbyService.getLobby(hostId);
        assertNotNull(lobby);
        assertEquals(hostId, lobby.getHost());
        assertEquals(4, lobby.getSize());
        assertEquals("test-game", lobby.getGame());
    }

    @Test
    @DisplayName("Create lobby with custom name")
    void testCreateLobby_withCustomName() {
        var command = createLobbyCommand(hostId, hostName, 4, false, "test-game", "My Custom Lobby");

        lobbyService.createLobby(command);

        Lobby lobby = lobbyService.getLobby(hostId);
        assertEquals("My Custom Lobby", lobby.getName());
    }

    @Test
    @DisplayName("Create lobby with default name when not specified")
    void testCreateLobby_defaultName() {
        var command = createLobbyCommand(hostId, hostName, 4, false, "test-game", null);

        lobbyService.createLobby(command);

        Lobby lobby = lobbyService.getLobby(hostId);
        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Creating new lobby removes host from previous lobbies")
    void testCreateLobby_removesFromPreviousLobbies() {
        // Create first lobby
        var command1 = createLobbyCommand(hostId, hostName, 4, false, "game1", null);
        lobbyService.createLobby(command1);

        // Join another lobby as a player
        UUID otherHostId = UUID.randomUUID();
        var otherCommand = createLobbyCommand(otherHostId, "OtherHost", 4, false, "game2", null);
        lobbyService.createLobby(otherCommand);

        var joinCommand = joinLobbyCommand(hostId, hostName, otherHostId);
        lobbyService.joinLobby(joinCommand);

        // Create a new lobby as host - should remove from other lobby
        var command2 = createLobbyCommand(hostId, hostName, 4, false, "game3", null);
        lobbyService.createLobby(command2);

        Lobby otherLobby = lobbyService.getLobby(otherHostId);
        assertFalse(otherLobby.getPlayerIds().contains(hostId));
    }

    @Test
    @DisplayName("Join lobby successfully")
    void testJoinLobby_success() {
        var createCommand = createLobbyCommand(hostId, hostName, 4, false, "test-game", null);
        lobbyService.createLobby(createCommand);

        UUID playerId = UUID.randomUUID();
        var joinCommand = joinLobbyCommand(playerId, "Player2", hostId);
        Lobby result = lobbyService.joinLobby(joinCommand);

        assertNotNull(result);
        assertEquals(2, result.getPlayers().size());
        assertTrue(result.getPlayerIds().contains(playerId));
    }

    @Test
    @DisplayName("Join lobby returns null for non-existent lobby")
    void testJoinLobby_lobbyNotFound() {
        UUID playerId = UUID.randomUUID();
        UUID nonExistentLobbyId = UUID.randomUUID();
        var joinCommand = joinLobbyCommand(playerId, "Player2", nonExistentLobbyId);

        Lobby result = lobbyService.joinLobby(joinCommand);

        assertNull(result);
    }

    @Test
    @DisplayName("Join lobby returns null when already in lobby")
    void testJoinLobby_alreadyInLobby() {
        var createCommand = createLobbyCommand(hostId, hostName, 4, false, "test-game", null);
        lobbyService.createLobby(createCommand);

        // Try to join as host (already in lobby)
        var joinCommand = joinLobbyCommand(hostId, hostName, hostId);
        Lobby result = lobbyService.joinLobby(joinCommand);

        assertNull(result);
    }

    @Test
    @DisplayName("Get lobbies filters out started lobbies")
    void testGetLobbies_filtersStarted() {
        // Create two lobbies
        var command1 = createLobbyCommand(hostId, hostName, 4, false, "game1", null);
        lobbyService.createLobby(command1);

        UUID host2 = UUID.randomUUID();
        var command2 = createLobbyCommand(host2, "Host2", 4, false, "game2", null);
        lobbyService.createLobby(command2);

        // Start one lobby
        Lobby lobby1 = lobbyService.getLobby(hostId);
        lobby1.start();

        List<Lobby> lobbies = lobbyService.getLobbies();

        assertEquals(1, lobbies.size());
        assertEquals(host2, lobbies.get(0).getHost());
    }

    @Test
    @DisplayName("Get lobbies returns all active lobbies")
    void testGetLobbies_returnsActiveLobbies() {
        var command1 = createLobbyCommand(hostId, hostName, 4, false, "game1", null);
        lobbyService.createLobby(command1);

        UUID host2 = UUID.randomUUID();
        var command2 = createLobbyCommand(host2, "Host2", 4, false, "game2", null);
        lobbyService.createLobby(command2);

        List<Lobby> lobbies = lobbyService.getLobbies();

        assertEquals(2, lobbies.size());
    }

    @Test
    @DisplayName("Find lobby by player")
    void testFindLobbyByPlayer() {
        var createCommand = createLobbyCommand(hostId, hostName, 4, false, "test-game", null);
        lobbyService.createLobby(createCommand);

        UUID playerId = UUID.randomUUID();
        var joinCommand = joinLobbyCommand(playerId, "Player2", hostId);
        lobbyService.joinLobby(joinCommand);

        Lobby foundLobby = lobbyService.findLobbyByPlayer(playerId);

        assertNotNull(foundLobby);
        assertEquals(hostId, foundLobby.getHost());
    }

    @Test
    @DisplayName("Find lobby by player returns null when not in any lobby")
    void testFindLobbyByPlayer_notInLobby() {
        UUID playerId = UUID.randomUUID();

        Lobby foundLobby = lobbyService.findLobbyByPlayer(playerId);

        assertNull(foundLobby);
    }

    @Test
    @DisplayName("Remove lobby")
    void testRemoveLobby() {
        var createCommand = createLobbyCommand(hostId, hostName, 4, false, "test-game", null);
        lobbyService.createLobby(createCommand);

        assertNotNull(lobbyService.getLobby(hostId));

        lobbyService.removeLobby(hostId);

        assertNull(lobbyService.getLobby(hostId));
    }

    @Test
    @DisplayName("Get lobby returns null for non-existent lobby")
    void testGetLobby_notFound() {
        Lobby lobby = lobbyService.getLobby(UUID.randomUUID());

        assertNull(lobby);
    }

    @Test
    @DisplayName("Hidden lobbies are still accessible but should be filtered by caller")
    void testHiddenLobbies() {
        var command = createLobbyCommand(hostId, hostName, 4, true, "test-game", null);
        lobbyService.createLobby(command);

        Lobby lobby = lobbyService.getLobby(hostId);
        assertNotNull(lobby);
        assertTrue(lobby.isHidden());

        // Note: getLobbies() doesn't filter hidden lobbies - that should be done at the handler level
        List<Lobby> lobbies = lobbyService.getLobbies();
        assertEquals(1, lobbies.size());
    }
}
