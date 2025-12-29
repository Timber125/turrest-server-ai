-- Player persistent statistics and leaderboards for Turrest02

-- Player lifetime statistics
CREATE TABLE IF NOT EXISTS PLAYER_STATS (
    user_id uuid PRIMARY KEY,
    total_games_played int DEFAULT 0,
    total_wins int DEFAULT 0,
    total_losses int DEFAULT 0,
    total_creeps_killed bigint DEFAULT 0,
    total_creeps_sent bigint DEFAULT 0,
    total_gold_earned bigint DEFAULT 0,
    total_gold_spent bigint DEFAULT 0,
    total_damage_dealt bigint DEFAULT 0,
    total_damage_taken bigint DEFAULT 0,
    total_towers_placed int DEFAULT 0,
    total_buildings_placed int DEFAULT 0,
    xp bigint DEFAULT 0,
    current_win_streak int DEFAULT 0,
    best_win_streak int DEFAULT 0,
    last_game_at timestamp,
    created_at timestamp DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES USERPROFILE(id)
);

-- Match history for individual games
CREATE TABLE IF NOT EXISTS MATCH_HISTORY (
    id uuid DEFAULT random_uuid() PRIMARY KEY,
    game_id uuid NOT NULL,
    user_id uuid NOT NULL,
    player_number int NOT NULL,
    is_winner boolean DEFAULT false,
    final_hp int DEFAULT 0,
    creeps_killed int DEFAULT 0,
    creeps_sent int DEFAULT 0,
    gold_earned bigint DEFAULT 0,
    gold_spent bigint DEFAULT 0,
    damage_dealt bigint DEFAULT 0,
    damage_taken bigint DEFAULT 0,
    towers_placed int DEFAULT 0,
    buildings_placed int DEFAULT 0,
    xp_earned int DEFAULT 0,
    game_duration_ms bigint DEFAULT 0,
    played_at timestamp DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES USERPROFILE(id)
);

-- Index for fast leaderboard queries
CREATE INDEX IF NOT EXISTS idx_player_stats_xp ON PLAYER_STATS(xp DESC);
CREATE INDEX IF NOT EXISTS idx_player_stats_wins ON PLAYER_STATS(total_wins DESC);
CREATE INDEX IF NOT EXISTS idx_player_stats_win_streak ON PLAYER_STATS(best_win_streak DESC);
CREATE INDEX IF NOT EXISTS idx_match_history_user ON MATCH_HISTORY(user_id);
CREATE INDEX IF NOT EXISTS idx_match_history_game ON MATCH_HISTORY(game_id);
CREATE INDEX IF NOT EXISTS idx_match_history_played ON MATCH_HISTORY(played_at DESC);
