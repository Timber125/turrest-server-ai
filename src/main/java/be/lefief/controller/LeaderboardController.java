package be.lefief.controller;

import be.lefief.repository.turrest02.PersistentPlayerStats;
import be.lefief.service.turrest02.PersistentStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final PersistentStatsService statsService;

    public LeaderboardController(PersistentStatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * Get top players by XP (level).
     */
    @GetMapping("/xp")
    public ResponseEntity<List<LeaderboardEntry>> getXpLeaderboard() {
        List<PersistentPlayerStats> stats = statsService.getXpLeaderboard(100);
        List<LeaderboardEntry> entries = stats.stream()
                .map(s -> new LeaderboardEntry(
                        s.getUserId(),
                        null, // Name not available from stats alone
                        s.getLevel(),
                        s.getXp(),
                        s.getTotalWins(),
                        s.getTotalGamesPlayed(),
                        s.getWinRate(),
                        s.getBestWinStreak()
                ))
                .toList();
        return ResponseEntity.ok(entries);
    }

    /**
     * Get top players by wins.
     */
    @GetMapping("/wins")
    public ResponseEntity<List<LeaderboardEntry>> getWinsLeaderboard() {
        List<PersistentPlayerStats> stats = statsService.getWinsLeaderboard(100);
        List<LeaderboardEntry> entries = stats.stream()
                .map(s -> new LeaderboardEntry(
                        s.getUserId(),
                        null,
                        s.getLevel(),
                        s.getXp(),
                        s.getTotalWins(),
                        s.getTotalGamesPlayed(),
                        s.getWinRate(),
                        s.getBestWinStreak()
                ))
                .toList();
        return ResponseEntity.ok(entries);
    }

    /**
     * Get stats for a specific player.
     */
    @GetMapping("/player/{userId}")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(@PathVariable UUID userId) {
        return statsService.getPlayerStats(userId)
                .map(s -> ResponseEntity.ok(new PlayerStatsResponse(
                        s.getUserId(),
                        s.getLevel(),
                        s.getXp(),
                        s.getTotalGamesPlayed(),
                        s.getTotalWins(),
                        s.getTotalLosses(),
                        s.getWinRate(),
                        s.getCurrentWinStreak(),
                        s.getBestWinStreak(),
                        s.getTotalCreepsKilled(),
                        s.getTotalCreepsSent(),
                        s.getTotalDamageDealt(),
                        s.getTotalDamageTaken(),
                        s.getTotalTowersPlaced(),
                        s.getTotalBuildingsPlaced()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // DTOs
    public record LeaderboardEntry(
            UUID userId,
            String playerName,
            int level,
            long xp,
            int wins,
            int gamesPlayed,
            double winRate,
            int bestWinStreak
    ) {}

    public record PlayerStatsResponse(
            UUID userId,
            int level,
            long xp,
            int totalGamesPlayed,
            int totalWins,
            int totalLosses,
            double winRate,
            int currentWinStreak,
            int bestWinStreak,
            long totalCreepsKilled,
            long totalCreepsSent,
            long totalDamageDealt,
            long totalDamageTaken,
            int totalTowersPlaced,
            int totalBuildingsPlaced
    ) {}
}
