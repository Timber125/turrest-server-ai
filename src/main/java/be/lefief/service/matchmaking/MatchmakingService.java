package be.lefief.service.matchmaking;

import be.lefief.lobby.LobbyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for player matchmaking.
 */
@Service
public class MatchmakingService {

    private static final Logger LOG = LoggerFactory.getLogger(MatchmakingService.class);

    private final MatchmakingQueue rankedQueue = new MatchmakingQueue(MatchmakingQueue.GameMode.RANKED);
    private final MatchmakingQueue casualQueue = new MatchmakingQueue(MatchmakingQueue.GameMode.CASUAL);
    private final Map<UUID, MatchmakingQueue.GameMode> playerModes = new ConcurrentHashMap<>();

    private final EloService eloService;
    private final LobbyManager lobbyManager;

    // Callbacks for when matches are found
    private final Map<UUID, MatchFoundCallback> matchCallbacks = new ConcurrentHashMap<>();

    public MatchmakingService(EloService eloService, LobbyManager lobbyManager) {
        this.eloService = eloService;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Join the matchmaking queue.
     *
     * @param userId User's ID
     * @param userName User's display name
     * @param eloRating User's ELO rating
     * @param mode Game mode (RANKED or CASUAL)
     * @param callback Callback when match is found
     */
    public void joinQueue(UUID userId, String userName, int eloRating,
                          MatchmakingQueue.GameMode mode, MatchFoundCallback callback) {
        // Remove from any existing queue
        leaveQueue(userId);

        MatchmakingQueue queue = mode == MatchmakingQueue.GameMode.RANKED ? rankedQueue : casualQueue;
        queue.enqueue(userId, userName, eloRating);
        playerModes.put(userId, mode);
        matchCallbacks.put(userId, callback);

        LOG.info("Player {} joined {} matchmaking", userName, mode);
    }

    /**
     * Leave the matchmaking queue.
     */
    public void leaveQueue(UUID userId) {
        rankedQueue.dequeue(userId);
        casualQueue.dequeue(userId);
        playerModes.remove(userId);
        matchCallbacks.remove(userId);
    }

    /**
     * Check if a player is in queue.
     */
    public boolean isInQueue(UUID userId) {
        return rankedQueue.isQueued(userId) || casualQueue.isQueued(userId);
    }

    /**
     * Get queue status for a player.
     */
    public QueueStatus getQueueStatus(UUID userId, int eloRating) {
        MatchmakingQueue.GameMode mode = playerModes.get(userId);
        if (mode == null) {
            return new QueueStatus(false, null, 0, 0);
        }

        MatchmakingQueue queue = mode == MatchmakingQueue.GameMode.RANKED ? rankedQueue : casualQueue;
        return new QueueStatus(true, mode, queue.size(), queue.estimateWaitTime(eloRating));
    }

    /**
     * Scheduled task to process matchmaking queues.
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void processQueues() {
        processQueue(rankedQueue, MatchmakingQueue.GameMode.RANKED);
        processQueue(casualQueue, MatchmakingQueue.GameMode.CASUAL);
    }

    private void processQueue(MatchmakingQueue queue, MatchmakingQueue.GameMode mode) {
        List<MatchmakingQueue.QueueEntry> matched = queue.tryMatch();
        if (!matched.isEmpty() && matched.size() >= 2) {
            MatchmakingQueue.QueueEntry player1 = matched.get(0);
            MatchmakingQueue.QueueEntry player2 = matched.get(1);

            LOG.info("Match found in {} queue: {} vs {}",
                    mode, player1.getUserName(), player2.getUserName());

            // Notify players
            MatchFoundCallback callback1 = matchCallbacks.remove(player1.getUserId());
            MatchFoundCallback callback2 = matchCallbacks.remove(player2.getUserId());
            playerModes.remove(player1.getUserId());
            playerModes.remove(player2.getUserId());

            MatchInfo matchInfo = new MatchInfo(
                    UUID.randomUUID(),
                    mode,
                    player1,
                    player2
            );

            if (callback1 != null) {
                callback1.onMatchFound(matchInfo);
            }
            if (callback2 != null) {
                callback2.onMatchFound(matchInfo);
            }
        }
    }

    /**
     * Get total players in queue across all modes.
     */
    public int getTotalQueueSize() {
        return rankedQueue.size() + casualQueue.size();
    }

    /**
     * Callback interface for match found events.
     */
    @FunctionalInterface
    public interface MatchFoundCallback {
        void onMatchFound(MatchInfo matchInfo);
    }

    /**
     * Queue status information.
     */
    public record QueueStatus(
            boolean inQueue,
            MatchmakingQueue.GameMode mode,
            int playersInQueue,
            int estimatedWaitSeconds
    ) {}

    /**
     * Match information.
     */
    public record MatchInfo(
            UUID matchId,
            MatchmakingQueue.GameMode mode,
            MatchmakingQueue.QueueEntry player1,
            MatchmakingQueue.QueueEntry player2
    ) {}
}
