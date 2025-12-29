package be.lefief.service.matchmaking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ELO rating calculation service.
 * Based on the Elo rating system used in chess.
 */
@Service
public class EloService {

    private static final Logger LOG = LoggerFactory.getLogger(EloService.class);

    // K-factor: determines how much ratings change per game
    private static final int K_FACTOR_NEW_PLAYER = 40;    // First 10 games
    private static final int K_FACTOR_ESTABLISHED = 20;   // After 10 games
    private static final int K_FACTOR_EXPERIENCED = 10;   // After 100 games

    // Starting ELO for new players
    public static final int STARTING_ELO = 1000;

    /**
     * Calculate new ELO ratings after a game.
     *
     * @param winnerElo Winner's current ELO
     * @param loserElo Loser's current ELO
     * @param winnerGamesPlayed Winner's total games played
     * @param loserGamesPlayed Loser's total games played
     * @return Array of [newWinnerElo, newLoserElo]
     */
    public int[] calculateNewRatings(int winnerElo, int loserElo, int winnerGamesPlayed, int loserGamesPlayed) {
        // Calculate expected scores
        double winnerExpected = expectedScore(winnerElo, loserElo);
        double loserExpected = expectedScore(loserElo, winnerElo);

        // Get K-factors
        int winnerK = getKFactor(winnerGamesPlayed);
        int loserK = getKFactor(loserGamesPlayed);

        // Calculate new ratings
        // Winner scored 1.0, loser scored 0.0
        int newWinnerElo = (int) Math.round(winnerElo + winnerK * (1.0 - winnerExpected));
        int newLoserElo = (int) Math.round(loserElo + loserK * (0.0 - loserExpected));

        // Prevent going below minimum rating
        newLoserElo = Math.max(100, newLoserElo);

        LOG.debug("ELO update: Winner {} -> {} (expected {:.2f}), Loser {} -> {} (expected {:.2f})",
                winnerElo, newWinnerElo, winnerExpected, loserElo, newLoserElo, loserExpected);

        return new int[]{newWinnerElo, newLoserElo};
    }

    /**
     * Calculate expected score (probability of winning) for player A against player B.
     */
    public double expectedScore(int playerARating, int playerBRating) {
        return 1.0 / (1.0 + Math.pow(10, (playerBRating - playerARating) / 400.0));
    }

    /**
     * Get K-factor based on games played.
     * Higher K-factor means ratings change more quickly.
     */
    public int getKFactor(int gamesPlayed) {
        if (gamesPlayed < 10) {
            return K_FACTOR_NEW_PLAYER;
        } else if (gamesPlayed < 100) {
            return K_FACTOR_ESTABLISHED;
        } else {
            return K_FACTOR_EXPERIENCED;
        }
    }

    /**
     * Calculate ELO change for a match (for display purposes).
     */
    public int calculateEloChange(int playerElo, int opponentElo, boolean won, int gamesPlayed) {
        double expected = expectedScore(playerElo, opponentElo);
        int k = getKFactor(gamesPlayed);
        double actual = won ? 1.0 : 0.0;
        return (int) Math.round(k * (actual - expected));
    }
}
