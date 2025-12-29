package be.lefief.repository.turrest02;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PersistentPlayerStats {
    private final UUID userId;
    private int totalGamesPlayed;
    private int totalWins;
    private int totalLosses;
    private long totalCreepsKilled;
    private long totalCreepsSent;
    private long totalGoldEarned;
    private long totalGoldSpent;
    private long totalDamageDealt;
    private long totalDamageTaken;
    private int totalTowersPlaced;
    private int totalBuildingsPlaced;
    private long xp;
    private int currentWinStreak;
    private int bestWinStreak;
    private LocalDateTime lastGameAt;
    private LocalDateTime createdAt;

    /**
     * Calculate player level from XP.
     * Level formula: sqrt(xp / 100)
     */
    public int getLevel() {
        return (int) Math.floor(Math.sqrt(xp / 100.0)) + 1;
    }

    /**
     * Get win rate as percentage.
     */
    public double getWinRate() {
        if (totalGamesPlayed == 0) return 0.0;
        return (double) totalWins / totalGamesPlayed * 100.0;
    }
}
