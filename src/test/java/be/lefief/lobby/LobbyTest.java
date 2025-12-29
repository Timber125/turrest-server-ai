package be.lefief.lobby;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lobby Tests")
class LobbyTest {

    private UUID hostId;
    private String hostName;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        hostName = "TestHost";
    }

    @Test
    @DisplayName("Create lobby with default values")
    void testCreateLobby_defaultValues() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        assertEquals(hostId, lobby.getHost());
        assertEquals(hostId, lobby.getLobbyID());
        assertEquals("test-game", lobby.getGame());
        assertEquals(4, lobby.getSize());
        assertFalse(lobby.isHidden());
        assertFalse(lobby.isStarted());
        assertTrue(lobby.isOpen());
        assertEquals(1, lobby.getPlayers().size());
        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Create lobby with custom name")
    void testCreateLobby_customName() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game", "Custom Lobby Name");

        assertEquals("Custom Lobby Name", lobby.getName());
    }

    @Test
    @DisplayName("Create lobby with empty name defaults to host's lobby")
    void testCreateLobby_emptyNameDefaultsToHostLobby() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game", "");

        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Create lobby with whitespace name defaults to host's lobby")
    void testCreateLobby_whitespaceNameDefaultsToHostLobby() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game", "   ");

        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Lobby name is truncated to 50 characters")
    void testCreateLobby_nameTruncation() {
        String longName = "A".repeat(100);
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game", longName);

        assertEquals(50, lobby.getName().length());
        assertEquals("A".repeat(50), lobby.getName());
    }

    @Test
    @DisplayName("Single player lobby is not open")
    void testCreateLobby_singlePlayerNotOpen() {
        Lobby lobby = new Lobby(hostId, hostName, 1, false, "test-game");

        assertFalse(lobby.isOpen());
    }

    @Test
    @DisplayName("Host gets color index 0")
    void testCreateLobby_hostGetsColorZero() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        LobbyPlayer host = lobby.getPlayer(hostId);
        assertNotNull(host);
        assertEquals(0, host.getColorIndex());
    }

    @Test
    @DisplayName("Add client successfully")
    void testAddClient_success() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();

        boolean result = lobby.addClient(clientId, "Player2");

        assertTrue(result);
        assertEquals(2, lobby.getPlayers().size());
        assertNotNull(lobby.getPlayer(clientId));
    }

    @Test
    @DisplayName("New client gets next available color")
    void testAddClient_getsNextAvailableColor() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();

        lobby.addClient(clientId, "Player2");

        LobbyPlayer player = lobby.getPlayer(clientId);
        assertEquals(1, player.getColorIndex());
    }

    @Test
    @DisplayName("Cannot add client to full lobby")
    void testAddClient_lobbyFull() {
        Lobby lobby = new Lobby(hostId, hostName, 2, false, "test-game");
        UUID client1 = UUID.randomUUID();
        UUID client2 = UUID.randomUUID();

        lobby.addClient(client1, "Player2");
        boolean result = lobby.addClient(client2, "Player3");

        assertFalse(result);
        assertEquals(2, lobby.getPlayers().size());
    }

    @Test
    @DisplayName("Lobby closes when full")
    void testAddClient_lobbyClosesWhenFull() {
        Lobby lobby = new Lobby(hostId, hostName, 2, false, "test-game");
        UUID clientId = UUID.randomUUID();

        assertTrue(lobby.isOpen());
        lobby.addClient(clientId, "Player2");
        assertFalse(lobby.isOpen());
    }

    @Test
    @DisplayName("Cannot add client to started lobby")
    void testAddClient_lobbyStarted() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        lobby.getPlayer(hostId).setReady(true);
        lobby.start();

        UUID clientId = UUID.randomUUID();
        boolean result = lobby.addClient(clientId, "Player2");

        assertFalse(result);
    }

    @Test
    @DisplayName("Cannot add same client twice")
    void testAddClient_duplicateClient() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();

        assertTrue(lobby.addClient(clientId, "Player2"));
        assertFalse(lobby.addClient(clientId, "Player2"));
        assertEquals(2, lobby.getPlayers().size());
    }

    @Test
    @DisplayName("Remove non-host client")
    void testRemoveClient_normalPlayer() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        boolean result = lobby.removeClient(clientId);

        assertTrue(result);
        assertEquals(1, lobby.getPlayers().size());
        assertNull(lobby.getPlayer(clientId));
    }

    @Test
    @DisplayName("Lobby reopens when player leaves")
    void testRemoveClient_lobbyReopens() {
        Lobby lobby = new Lobby(hostId, hostName, 2, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");
        assertFalse(lobby.isOpen());

        lobby.removeClient(clientId);

        assertTrue(lobby.isOpen());
    }

    @Test
    @DisplayName("Host leaving destroys lobby")
    void testRemoveClient_host() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        boolean result = lobby.removeClient(hostId);

        assertTrue(result);
        assertEquals(0, lobby.getPlayers().size());
        assertFalse(lobby.isOpen());
    }

    @Test
    @DisplayName("Change player color to valid color")
    void testChangePlayerColor_validColor() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        boolean result = lobby.changePlayerColor(hostId, 5);

        assertTrue(result);
        assertEquals(5, lobby.getPlayer(hostId).getColorIndex());
    }

    @Test
    @DisplayName("Cannot change to taken color")
    void testChangePlayerColor_colorTaken() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        // Host has color 0, player2 has color 1
        boolean result = lobby.changePlayerColor(clientId, 0);

        assertFalse(result);
        assertEquals(1, lobby.getPlayer(clientId).getColorIndex());
    }

    @Test
    @DisplayName("Cannot change color when ready")
    void testChangePlayerColor_playerReady() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        lobby.getPlayer(hostId).setReady(true);

        boolean result = lobby.changePlayerColor(hostId, 5);

        assertFalse(result);
        assertEquals(0, lobby.getPlayer(hostId).getColorIndex());
    }

    @Test
    @DisplayName("Cannot change color for non-existent player")
    void testChangePlayerColor_playerNotFound() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID unknownId = UUID.randomUUID();

        boolean result = lobby.changePlayerColor(unknownId, 5);

        assertFalse(result);
    }

    @Test
    @DisplayName("Toggle player ready state")
    void testTogglePlayerReady() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        assertFalse(lobby.getPlayer(hostId).isReady());

        boolean result1 = lobby.togglePlayerReady(hostId);
        assertTrue(result1);
        assertTrue(lobby.getPlayer(hostId).isReady());

        boolean result2 = lobby.togglePlayerReady(hostId);
        assertTrue(result2);
        assertFalse(lobby.getPlayer(hostId).isReady());
    }

    @Test
    @DisplayName("Toggle ready for non-existent player fails")
    void testTogglePlayerReady_playerNotFound() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID unknownId = UUID.randomUUID();

        boolean result = lobby.togglePlayerReady(unknownId);

        assertFalse(result);
    }

    @Test
    @DisplayName("All players ready returns true when all ready")
    void testAllPlayersReady_allReady() {
        Lobby lobby = new Lobby(hostId, hostName, 2, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        lobby.getPlayer(hostId).setReady(true);
        lobby.getPlayer(clientId).setReady(true);

        assertTrue(lobby.allPlayersReady());
    }

    @Test
    @DisplayName("All players ready returns false when some not ready")
    void testAllPlayersReady_someNotReady() {
        Lobby lobby = new Lobby(hostId, hostName, 2, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        lobby.getPlayer(hostId).setReady(true);
        // Player2 not ready

        assertFalse(lobby.allPlayersReady());
    }

    @Test
    @DisplayName("Start lobby successfully")
    void testStart() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        assertFalse(lobby.isStarted());
        assertTrue(lobby.isOpen());

        lobby.start();

        assertTrue(lobby.isStarted());
        assertFalse(lobby.isOpen());
    }

    @Test
    @DisplayName("Host can rename lobby")
    void testSetName_asHost() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        boolean result = lobby.setName("New Lobby Name", hostId);

        assertTrue(result);
        assertEquals("New Lobby Name", lobby.getName());
    }

    @Test
    @DisplayName("Non-host cannot rename lobby")
    void testSetName_notHost() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        boolean result = lobby.setName("New Name", clientId);

        assertFalse(result);
        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Cannot rename started lobby")
    void testSetName_lobbyStarted() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        lobby.start();

        boolean result = lobby.setName("New Name", hostId);

        assertFalse(result);
        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Cannot set empty lobby name")
    void testSetName_emptyName() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        boolean result = lobby.setName("", hostId);

        assertFalse(result);
        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Cannot set null lobby name")
    void testSetName_nullName() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        boolean result = lobby.setName(null, hostId);

        assertFalse(result);
        assertEquals(hostName + "'s lobby", lobby.getName());
    }

    @Test
    @DisplayName("Rename trims whitespace")
    void testSetName_trimsWhitespace() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");

        boolean result = lobby.setName("  New Name  ", hostId);

        assertTrue(result);
        assertEquals("New Name", lobby.getName());
    }

    @Test
    @DisplayName("Rename truncates long names")
    void testSetName_truncatesLongName() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        String longName = "B".repeat(100);

        boolean result = lobby.setName(longName, hostId);

        assertTrue(result);
        assertEquals(50, lobby.getName().length());
    }

    @Test
    @DisplayName("Get player IDs returns all player IDs")
    void testGetPlayerIds() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID client1 = UUID.randomUUID();
        UUID client2 = UUID.randomUUID();
        lobby.addClient(client1, "Player2");
        lobby.addClient(client2, "Player3");

        var playerIds = lobby.getPlayerIds();

        assertEquals(3, playerIds.size());
        assertTrue(playerIds.contains(hostId));
        assertTrue(playerIds.contains(client1));
        assertTrue(playerIds.contains(client2));
    }

    @Test
    @DisplayName("Is color taken correctly identifies taken colors")
    void testIsColorTaken() {
        Lobby lobby = new Lobby(hostId, hostName, 4, false, "test-game");
        UUID clientId = UUID.randomUUID();
        lobby.addClient(clientId, "Player2");

        // Host has color 0
        assertTrue(lobby.isColorTaken(0, clientId));
        // Color 1 is taken by client
        assertTrue(lobby.isColorTaken(1, hostId));
        // Color 2 is free
        assertFalse(lobby.isColorTaken(2, hostId));
        // A player's own color is not "taken" for themselves
        assertFalse(lobby.isColorTaken(0, hostId));
    }

    @Test
    @DisplayName("Hidden lobby property is set correctly")
    void testHiddenLobby() {
        Lobby hiddenLobby = new Lobby(hostId, hostName, 4, true, "test-game");
        Lobby publicLobby = new Lobby(UUID.randomUUID(), "Other", 4, false, "test-game");

        assertTrue(hiddenLobby.isHidden());
        assertFalse(publicLobby.isHidden());
    }
}
