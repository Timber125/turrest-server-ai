package be.lefief.lobby;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LobbyPlayer Tests")
class LobbyPlayerTest {

    @Test
    @DisplayName("Create lobby player with correct values")
    void testCreateLobbyPlayer() {
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";
        int colorIndex = 5;

        LobbyPlayer player = new LobbyPlayer(playerId, playerName, colorIndex);

        assertEquals(playerId, player.getId());
        assertEquals(playerName, player.getName());
        assertEquals(colorIndex, player.getColorIndex());
        assertFalse(player.isReady());
    }

    @Test
    @DisplayName("Set ready state")
    void testSetReady() {
        LobbyPlayer player = new LobbyPlayer(UUID.randomUUID(), "Test", 0);

        assertFalse(player.isReady());

        player.setReady(true);
        assertTrue(player.isReady());

        player.setReady(false);
        assertFalse(player.isReady());
    }

    @Test
    @DisplayName("Set color index")
    void testSetColorIndex() {
        LobbyPlayer player = new LobbyPlayer(UUID.randomUUID(), "Test", 0);

        assertEquals(0, player.getColorIndex());

        player.setColorIndex(10);
        assertEquals(10, player.getColorIndex());
    }
}
