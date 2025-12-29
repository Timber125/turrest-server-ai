package be.lefief.game;

import be.lefief.sockets.ClientSession;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class Player {

    private static final long GRACE_PERIOD_MS = 60000; // 60 seconds grace period

    private ClientSession clientSession;
    private Integer playerNumber;
    private UUID gameID;
    private boolean connected = true;
    private int colorIndex;
    private Instant disconnectedAt;

    public Player(ClientSession clientSession, Integer playerNumber, UUID gameID, int colorIndex) {
        this.clientSession = clientSession;
        this.playerNumber = playerNumber;
        this.gameID = gameID;
        this.connected = true;
        this.colorIndex = colorIndex;
        this.disconnectedAt = null;
    }

    /**
     * Mark player as disconnected with timestamp.
     */
    public void markDisconnected() {
        this.connected = false;
        this.disconnectedAt = Instant.now();
    }

    /**
     * Mark player as reconnected.
     */
    public void markReconnected() {
        this.connected = true;
        this.disconnectedAt = null;
    }

    /**
     * Check if the grace period has expired for a disconnected player.
     */
    public boolean isGracePeriodExpired() {
        if (connected || disconnectedAt == null) {
            return false;
        }
        return Instant.now().toEpochMilli() - disconnectedAt.toEpochMilli() > GRACE_PERIOD_MS;
    }

    /**
     * Get remaining grace period in milliseconds.
     */
    public long getRemainingGracePeriodMs() {
        if (connected || disconnectedAt == null) {
            return GRACE_PERIOD_MS;
        }
        long elapsed = Instant.now().toEpochMilli() - disconnectedAt.toEpochMilli();
        return Math.max(0, GRACE_PERIOD_MS - elapsed);
    }

    /**
     * Get the player's score for ranking purposes.
     * Each game mode defines what "score" means.
     */
    public abstract int getScore();

    /**
     * Get the label for the score (e.g., "HP", "Points", "Gold").
     */
    public abstract String getScoreLabel();
}
