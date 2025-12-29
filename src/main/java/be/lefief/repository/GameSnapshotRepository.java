package be.lefief.repository;

import be.lefief.game.persistence.GameSnapshot;
import be.lefief.game.persistence.GameStateSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for game snapshot persistence.
 */
@Repository
public class GameSnapshotRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GameSnapshotRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final GameStateSerializer serializer;

    public GameSnapshotRepository(NamedParameterJdbcTemplate jdbcTemplate, GameStateSerializer serializer) {
        this.jdbcTemplate = jdbcTemplate;
        this.serializer = serializer;
    }

    /**
     * Save a game snapshot.
     */
    public void save(GameSnapshot snapshot) {
        String json = serializer.serialize(snapshot);
        if (json == null) {
            LOG.error("Failed to serialize snapshot for game {}", snapshot.getGameId());
            return;
        }

        String sql = """
            MERGE INTO game_snapshots (id, game_id, tick_number, snapshot_data, created_at)
            KEY (game_id, tick_number)
            VALUES (:id, :gameId, :tickNumber, :snapshotData, :createdAt)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", snapshot.getSnapshotId())
                .addValue("gameId", snapshot.getGameId())
                .addValue("tickNumber", snapshot.getTickNumber())
                .addValue("snapshotData", json)
                .addValue("createdAt", LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault()));

        jdbcTemplate.update(sql, params);
        LOG.debug("Saved snapshot for game {} at tick {}", snapshot.getGameId(), snapshot.getTickNumber());
    }

    /**
     * Get the latest snapshot for a game.
     */
    public Optional<GameSnapshot> getLatestSnapshot(UUID gameId) {
        String sql = """
            SELECT snapshot_data FROM game_snapshots
            WHERE game_id = :gameId
            ORDER BY tick_number DESC
            LIMIT 1
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("gameId", gameId);

        List<GameSnapshot> snapshots = jdbcTemplate.query(sql, params, this::mapSnapshot);
        return snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.get(0));
    }

    /**
     * Get a specific snapshot by game ID and tick number.
     */
    public Optional<GameSnapshot> getSnapshot(UUID gameId, int tickNumber) {
        String sql = """
            SELECT snapshot_data FROM game_snapshots
            WHERE game_id = :gameId AND tick_number = :tickNumber
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("gameId", gameId)
                .addValue("tickNumber", tickNumber);

        List<GameSnapshot> snapshots = jdbcTemplate.query(sql, params, this::mapSnapshot);
        return snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.get(0));
    }

    /**
     * Get all snapshots for a game (for replay).
     */
    public List<GameSnapshot> getAllSnapshots(UUID gameId) {
        String sql = """
            SELECT snapshot_data FROM game_snapshots
            WHERE game_id = :gameId
            ORDER BY tick_number ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("gameId", gameId);

        return jdbcTemplate.query(sql, params, this::mapSnapshot);
    }

    /**
     * Delete old snapshots (cleanup).
     */
    public int deleteSnapshotsOlderThan(Instant cutoff) {
        String sql = """
            DELETE FROM game_snapshots
            WHERE created_at < :cutoff
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cutoff", LocalDateTime.ofInstant(cutoff, ZoneId.systemDefault()));

        int deleted = jdbcTemplate.update(sql, params);
        LOG.info("Deleted {} old snapshots", deleted);
        return deleted;
    }

    /**
     * Delete all snapshots for a game.
     */
    public void deleteGameSnapshots(UUID gameId) {
        String sql = "DELETE FROM game_snapshots WHERE game_id = :gameId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("gameId", gameId);
        jdbcTemplate.update(sql, params);
    }

    private GameSnapshot mapSnapshot(ResultSet rs, int rowNum) throws SQLException {
        String json = rs.getString("snapshot_data");
        return serializer.deserialize(json);
    }
}
