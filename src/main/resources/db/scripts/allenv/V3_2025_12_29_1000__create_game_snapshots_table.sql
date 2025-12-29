-- Game snapshots for persistence and replay
CREATE TABLE game_snapshots (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    tick_number INT NOT NULL,
    snapshot_data CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Index for efficient game lookup
    CONSTRAINT idx_game_snapshots_game_id_tick
        UNIQUE (game_id, tick_number)
);

-- Index for cleanup of old snapshots
CREATE INDEX idx_game_snapshots_created_at ON game_snapshots(created_at);
