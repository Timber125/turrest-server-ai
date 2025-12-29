package be.lefief.repository.turrest02;

import be.lefief.game.turrest02.stats.PlayerStats;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlayerStatsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PlayerStatsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Update player stats after a game ends.
     * Uses MERGE (upsert) to create or update the record.
     */
    public void updateAfterGame(UUID userId, PlayerStats gameStats, boolean isWinner, int xpEarned) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("user_id", userId);
        params.addValue("games_delta", 1);
        params.addValue("wins_delta", isWinner ? 1 : 0);
        params.addValue("losses_delta", isWinner ? 0 : 1);
        params.addValue("creeps_killed_delta", gameStats.getCreepsKilled());
        params.addValue("creeps_sent_delta", gameStats.getCreepsSent());
        params.addValue("gold_earned_delta", gameStats.getGoldEarned());
        params.addValue("gold_spent_delta", gameStats.getGoldSpent());
        params.addValue("damage_dealt_delta", gameStats.getDamageDealt());
        params.addValue("damage_taken_delta", gameStats.getDamageTaken());
        params.addValue("towers_delta", gameStats.getTowersPlaced());
        params.addValue("buildings_delta", gameStats.getBuildingsPlaced());
        params.addValue("xp_delta", xpEarned);

        // Calculate new win streak
        int newStreak = isWinner ? getCurrentWinStreak(userId) + 1 : 0;
        params.addValue("current_win_streak", newStreak);

        jdbcTemplate.update(PlayerStatsHelper.UPSERT_STATS, params);
    }

    /**
     * Get current win streak for a player.
     */
    private int getCurrentWinStreak(UUID userId) {
        PersistentPlayerStats stats = findByUserId(userId).orElse(null);
        return stats != null ? stats.getCurrentWinStreak() : 0;
    }

    /**
     * Find stats for a specific user.
     */
    public Optional<PersistentPlayerStats> findByUserId(UUID userId) {
        return Optional.ofNullable(
                jdbcTemplate.query(
                        PlayerStatsHelper.FIND_BY_USER_ID,
                        PlayerStatsHelper.BY_USER_ID(userId),
                        PlayerStatsHelper.RESULT_SET_EXTRACTOR
                )
        );
    }

    /**
     * Get top players by XP (level leaderboard).
     */
    public List<PersistentPlayerStats> getTopByXp(int limit) {
        return jdbcTemplate.query(
                PlayerStatsHelper.FIND_TOP_BY_XP,
                PlayerStatsHelper.BY_LIMIT(limit),
                PlayerStatsHelper.ROW_MAPPER
        );
    }

    /**
     * Get top players by wins.
     */
    public List<PersistentPlayerStats> getTopByWins(int limit) {
        return jdbcTemplate.query(
                PlayerStatsHelper.FIND_TOP_BY_WINS,
                PlayerStatsHelper.BY_LIMIT(limit),
                PlayerStatsHelper.ROW_MAPPER
        );
    }
}
