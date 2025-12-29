package be.lefief.repository.turrest02;

import be.lefief.util.DateUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class PlayerStatsHelper {
    public static final String TABLE_NAME = "PLAYER_STATS";

    // Column names
    private static final String USER_ID = "user_id";
    private static final String TOTAL_GAMES_PLAYED = "total_games_played";
    private static final String TOTAL_WINS = "total_wins";
    private static final String TOTAL_LOSSES = "total_losses";
    private static final String TOTAL_CREEPS_KILLED = "total_creeps_killed";
    private static final String TOTAL_CREEPS_SENT = "total_creeps_sent";
    private static final String TOTAL_GOLD_EARNED = "total_gold_earned";
    private static final String TOTAL_GOLD_SPENT = "total_gold_spent";
    private static final String TOTAL_DAMAGE_DEALT = "total_damage_dealt";
    private static final String TOTAL_DAMAGE_TAKEN = "total_damage_taken";
    private static final String TOTAL_TOWERS_PLACED = "total_towers_placed";
    private static final String TOTAL_BUILDINGS_PLACED = "total_buildings_placed";
    private static final String XP = "xp";
    private static final String CURRENT_WIN_STREAK = "current_win_streak";
    private static final String BEST_WIN_STREAK = "best_win_streak";
    private static final String LAST_GAME_AT = "last_game_at";
    private static final String CREATED_AT = "created_at";

    private static String param(String column) {
        return ":" + column;
    }

    // UPSERT query - creates or updates stats
    public static final String UPSERT_STATS =
            "MERGE INTO " + TABLE_NAME + " AS t " +
            "USING (VALUES (" + param(USER_ID) + ")) AS s(user_id) " +
            "ON t.user_id = s.user_id " +
            "WHEN MATCHED THEN UPDATE SET " +
                TOTAL_GAMES_PLAYED + " = " + TOTAL_GAMES_PLAYED + " + " + param("games_delta") + ", " +
                TOTAL_WINS + " = " + TOTAL_WINS + " + " + param("wins_delta") + ", " +
                TOTAL_LOSSES + " = " + TOTAL_LOSSES + " + " + param("losses_delta") + ", " +
                TOTAL_CREEPS_KILLED + " = " + TOTAL_CREEPS_KILLED + " + " + param("creeps_killed_delta") + ", " +
                TOTAL_CREEPS_SENT + " = " + TOTAL_CREEPS_SENT + " + " + param("creeps_sent_delta") + ", " +
                TOTAL_GOLD_EARNED + " = " + TOTAL_GOLD_EARNED + " + " + param("gold_earned_delta") + ", " +
                TOTAL_GOLD_SPENT + " = " + TOTAL_GOLD_SPENT + " + " + param("gold_spent_delta") + ", " +
                TOTAL_DAMAGE_DEALT + " = " + TOTAL_DAMAGE_DEALT + " + " + param("damage_dealt_delta") + ", " +
                TOTAL_DAMAGE_TAKEN + " = " + TOTAL_DAMAGE_TAKEN + " + " + param("damage_taken_delta") + ", " +
                TOTAL_TOWERS_PLACED + " = " + TOTAL_TOWERS_PLACED + " + " + param("towers_delta") + ", " +
                TOTAL_BUILDINGS_PLACED + " = " + TOTAL_BUILDINGS_PLACED + " + " + param("buildings_delta") + ", " +
                XP + " = " + XP + " + " + param("xp_delta") + ", " +
                CURRENT_WIN_STREAK + " = " + param(CURRENT_WIN_STREAK) + ", " +
                BEST_WIN_STREAK + " = GREATEST(" + BEST_WIN_STREAK + ", " + param(CURRENT_WIN_STREAK) + "), " +
                LAST_GAME_AT + " = NOW() " +
            "WHEN NOT MATCHED THEN INSERT (" +
                USER_ID + ", " + TOTAL_GAMES_PLAYED + ", " + TOTAL_WINS + ", " + TOTAL_LOSSES + ", " +
                TOTAL_CREEPS_KILLED + ", " + TOTAL_CREEPS_SENT + ", " + TOTAL_GOLD_EARNED + ", " + TOTAL_GOLD_SPENT + ", " +
                TOTAL_DAMAGE_DEALT + ", " + TOTAL_DAMAGE_TAKEN + ", " + TOTAL_TOWERS_PLACED + ", " + TOTAL_BUILDINGS_PLACED + ", " +
                XP + ", " + CURRENT_WIN_STREAK + ", " + BEST_WIN_STREAK + ", " + LAST_GAME_AT +
            ") VALUES (" +
                param(USER_ID) + ", " + param("games_delta") + ", " + param("wins_delta") + ", " + param("losses_delta") + ", " +
                param("creeps_killed_delta") + ", " + param("creeps_sent_delta") + ", " + param("gold_earned_delta") + ", " + param("gold_spent_delta") + ", " +
                param("damage_dealt_delta") + ", " + param("damage_taken_delta") + ", " + param("towers_delta") + ", " + param("buildings_delta") + ", " +
                param("xp_delta") + ", " + param(CURRENT_WIN_STREAK) + ", " + param(CURRENT_WIN_STREAK) + ", NOW()" +
            ")";

    public static final String FIND_BY_USER_ID =
            "SELECT * FROM " + TABLE_NAME + " WHERE " + USER_ID + " = " + param(USER_ID);

    public static final String FIND_TOP_BY_XP =
            "SELECT ps.*, up.name as player_name FROM " + TABLE_NAME + " ps " +
            "JOIN USERPROFILE up ON ps.user_id = up.id " +
            "ORDER BY " + XP + " DESC LIMIT " + param("limit");

    public static final String FIND_TOP_BY_WINS =
            "SELECT ps.*, up.name as player_name FROM " + TABLE_NAME + " ps " +
            "JOIN USERPROFILE up ON ps.user_id = up.id " +
            "ORDER BY " + TOTAL_WINS + " DESC LIMIT " + param("limit");

    public static MapSqlParameterSource BY_USER_ID(UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(USER_ID, userId);
        return params;
    }

    public static MapSqlParameterSource BY_LIMIT(int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("limit", limit);
        return params;
    }

    public static final RowMapper<PersistentPlayerStats> ROW_MAPPER = (rs, rowNum) ->
            PersistentPlayerStats.builder()
                    .userId(Optional.ofNullable(rs.getString(USER_ID)).map(UUID::fromString).orElse(null))
                    .totalGamesPlayed(rs.getInt(TOTAL_GAMES_PLAYED))
                    .totalWins(rs.getInt(TOTAL_WINS))
                    .totalLosses(rs.getInt(TOTAL_LOSSES))
                    .totalCreepsKilled(rs.getLong(TOTAL_CREEPS_KILLED))
                    .totalCreepsSent(rs.getLong(TOTAL_CREEPS_SENT))
                    .totalGoldEarned(rs.getLong(TOTAL_GOLD_EARNED))
                    .totalGoldSpent(rs.getLong(TOTAL_GOLD_SPENT))
                    .totalDamageDealt(rs.getLong(TOTAL_DAMAGE_DEALT))
                    .totalDamageTaken(rs.getLong(TOTAL_DAMAGE_TAKEN))
                    .totalTowersPlaced(rs.getInt(TOTAL_TOWERS_PLACED))
                    .totalBuildingsPlaced(rs.getInt(TOTAL_BUILDINGS_PLACED))
                    .xp(rs.getLong(XP))
                    .currentWinStreak(rs.getInt(CURRENT_WIN_STREAK))
                    .bestWinStreak(rs.getInt(BEST_WIN_STREAK))
                    .lastGameAt(DateUtil.toLocalDateTime(rs.getTimestamp(LAST_GAME_AT)))
                    .createdAt(DateUtil.toLocalDateTime(rs.getTimestamp(CREATED_AT)))
                    .build();

    public static final ResultSetExtractor<PersistentPlayerStats> RESULT_SET_EXTRACTOR = rs -> {
        if (!rs.next()) return null;
        return ROW_MAPPER.mapRow(rs, 0);
    };
}
