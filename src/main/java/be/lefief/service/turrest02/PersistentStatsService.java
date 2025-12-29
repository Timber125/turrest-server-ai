package be.lefief.service.turrest02;

import be.lefief.game.turrest02.Turrest02Player;
import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.game.turrest02.stats.GameStats;
import be.lefief.game.turrest02.stats.PlayerStats;
import be.lefief.repository.turrest02.PersistentPlayerStats;
import be.lefief.repository.turrest02.PlayerStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting player statistics after games.
 */
@Service
public class PersistentStatsService {

    private static final Logger LOG = LoggerFactory.getLogger(PersistentStatsService.class);

    // XP rewards
    private static final int XP_PER_WIN = 100;
    private static final int XP_PER_LOSS = 25;
    private static final int XP_PER_CREEP_KILLED = 1;
    private static final int XP_PER_CREEP_SENT = 2;
    private static final int XP_WIN_STREAK_BONUS = 10;  // Per streak level

    private final PlayerStatsRepository statsRepository;

    public PersistentStatsService(PlayerStatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    /**
     * Record game results for all players when a game ends.
     *
     * @param game     The game that ended
     * @param winnerId User ID of the winner (null if draw)
     */
    public void recordGameEnd(TurrestGameMode02 game, UUID winnerId) {
        GameStats gameStats = game.getGameStats();

        for (Turrest02Player player : game.getPlayerByNumber().values()) {
            if (player.getClientSession() == null) continue;

            UUID userId = player.getClientSession().getUserId();
            PlayerStats playerGameStats = gameStats.getStats(player.getPlayerNumber());
            boolean isWinner = userId.equals(winnerId);

            int xpEarned = calculateXpEarned(playerGameStats, isWinner, userId);

            try {
                statsRepository.updateAfterGame(userId, playerGameStats, isWinner, xpEarned);
                LOG.info("Updated persistent stats for user {} - Winner: {}, XP earned: {}",
                        userId, isWinner, xpEarned);
            } catch (Exception e) {
                LOG.error("Failed to update persistent stats for user {}", userId, e);
            }
        }
    }

    /**
     * Calculate XP earned from a game.
     */
    private int calculateXpEarned(PlayerStats stats, boolean isWinner, UUID userId) {
        int xp = 0;

        // Base XP for win/loss
        xp += isWinner ? XP_PER_WIN : XP_PER_LOSS;

        // XP for creeps killed and sent
        xp += stats.getCreepsKilled() * XP_PER_CREEP_KILLED;
        xp += stats.getCreepsSent() * XP_PER_CREEP_SENT;

        // Win streak bonus
        if (isWinner) {
            Optional<PersistentPlayerStats> currentStats = statsRepository.findByUserId(userId);
            if (currentStats.isPresent()) {
                int streak = currentStats.get().getCurrentWinStreak() + 1;
                xp += streak * XP_WIN_STREAK_BONUS;
            }
        }

        return xp;
    }

    /**
     * Get persistent stats for a player.
     */
    public Optional<PersistentPlayerStats> getPlayerStats(UUID userId) {
        return statsRepository.findByUserId(userId);
    }

    /**
     * Get XP leaderboard.
     */
    public List<PersistentPlayerStats> getXpLeaderboard(int limit) {
        return statsRepository.getTopByXp(limit);
    }

    /**
     * Get wins leaderboard.
     */
    public List<PersistentPlayerStats> getWinsLeaderboard(int limit) {
        return statsRepository.getTopByWins(limit);
    }
}
