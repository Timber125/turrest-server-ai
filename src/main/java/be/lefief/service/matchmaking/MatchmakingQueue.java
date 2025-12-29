package be.lefief.service.matchmaking;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Queue for players waiting to be matched.
 */
public class MatchmakingQueue {

    private static final Logger LOG = LoggerFactory.getLogger(MatchmakingQueue.class);

    // ELO range for matching - expands over time
    private static final int INITIAL_ELO_RANGE = 100;
    private static final int ELO_RANGE_EXPANSION_PER_SECOND = 10;
    private static final int MAX_ELO_RANGE = 500;

    private final Map<UUID, QueueEntry> queue = new ConcurrentHashMap<>();
    private final GameMode gameMode;

    public MatchmakingQueue(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * Add a player to the queue.
     */
    public void enqueue(UUID userId, String userName, int eloRating) {
        QueueEntry entry = new QueueEntry(userId, userName, eloRating, Instant.now());
        queue.put(userId, entry);
        LOG.info("Player {} ({} ELO) joined {} queue", userName, eloRating, gameMode);
    }

    /**
     * Remove a player from the queue.
     */
    public void dequeue(UUID userId) {
        QueueEntry removed = queue.remove(userId);
        if (removed != null) {
            LOG.info("Player {} left {} queue", removed.getUserName(), gameMode);
        }
    }

    /**
     * Check if a player is in the queue.
     */
    public boolean isQueued(UUID userId) {
        return queue.containsKey(userId);
    }

    /**
     * Try to find a match for queued players.
     * @return List of matched user IDs (2 players), or empty if no match found
     */
    public List<QueueEntry> tryMatch() {
        if (queue.size() < 2) {
            return Collections.emptyList();
        }

        List<QueueEntry> entries = new ArrayList<>(queue.values());
        Instant now = Instant.now();

        // Sort by queue time (oldest first)
        entries.sort(Comparator.comparing(QueueEntry::getQueuedAt));

        // Try to match players
        for (int i = 0; i < entries.size(); i++) {
            QueueEntry player1 = entries.get(i);
            int player1Range = calculateEloRange(player1, now);

            for (int j = i + 1; j < entries.size(); j++) {
                QueueEntry player2 = entries.get(j);
                int player2Range = calculateEloRange(player2, now);

                int eloDiff = Math.abs(player1.getEloRating() - player2.getEloRating());

                // Check if both players are within each other's acceptable range
                if (eloDiff <= player1Range && eloDiff <= player2Range) {
                    // Match found!
                    queue.remove(player1.getUserId());
                    queue.remove(player2.getUserId());
                    LOG.info("Match found: {} ({} ELO) vs {} ({} ELO), diff: {}",
                            player1.getUserName(), player1.getEloRating(),
                            player2.getUserName(), player2.getEloRating(),
                            eloDiff);
                    return List.of(player1, player2);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Calculate acceptable ELO range based on time in queue.
     */
    private int calculateEloRange(QueueEntry entry, Instant now) {
        long secondsInQueue = now.getEpochSecond() - entry.getQueuedAt().getEpochSecond();
        int expandedRange = INITIAL_ELO_RANGE + (int) (secondsInQueue * ELO_RANGE_EXPANSION_PER_SECOND);
        return Math.min(expandedRange, MAX_ELO_RANGE);
    }

    /**
     * Get queue size.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Get estimated wait time in seconds for a player with given ELO.
     */
    public int estimateWaitTime(int eloRating) {
        if (queue.isEmpty()) {
            return 30; // Default estimate
        }

        // Find closest player by ELO
        int closestDiff = Integer.MAX_VALUE;
        for (QueueEntry entry : queue.values()) {
            int diff = Math.abs(entry.getEloRating() - eloRating);
            closestDiff = Math.min(closestDiff, diff);
        }

        // Estimate based on ELO difference and queue expansion rate
        if (closestDiff <= INITIAL_ELO_RANGE) {
            return 5; // Should match quickly
        } else {
            int secondsNeeded = (closestDiff - INITIAL_ELO_RANGE) / ELO_RANGE_EXPANSION_PER_SECOND;
            return Math.min(secondsNeeded, 60);
        }
    }

    @Data
    public static class QueueEntry {
        private final UUID userId;
        private final String userName;
        private final int eloRating;
        private final Instant queuedAt;
    }

    public enum GameMode {
        RANKED, CASUAL
    }
}
