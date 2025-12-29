package be.lefief.service.matchmaking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ELO rating calculations.
 */
@DisplayName("ELO Service Tests")
class EloServiceTest {

    private EloService eloService;

    @BeforeEach
    void setUp() {
        eloService = new EloService();
    }

    @Test
    @DisplayName("Equal rated players have 50% expected score")
    void equalRatedPlayersExpectedScore() {
        double expected = eloService.expectedScore(1000, 1000);
        assertEquals(0.5, expected, 0.001);
    }

    @Test
    @DisplayName("Higher rated player has higher expected score")
    void higherRatedPlayerExpectedScore() {
        double higherPlayerExpected = eloService.expectedScore(1200, 1000);
        double lowerPlayerExpected = eloService.expectedScore(1000, 1200);

        assertTrue(higherPlayerExpected > 0.5);
        assertTrue(lowerPlayerExpected < 0.5);
        assertEquals(1.0, higherPlayerExpected + lowerPlayerExpected, 0.001);
    }

    @Test
    @DisplayName("Winner gains rating, loser loses rating")
    void winnerGainsLoserLoses() {
        int[] newRatings = eloService.calculateNewRatings(1000, 1000, 50, 50);

        assertTrue(newRatings[0] > 1000, "Winner should gain rating");
        assertTrue(newRatings[1] < 1000, "Loser should lose rating");
    }

    @Test
    @DisplayName("Upset win gives more rating change")
    void upsetWinGivesMoreRating() {
        // Lower rated player beats higher rated player (upset)
        int[] upsetRatings = eloService.calculateNewRatings(800, 1200, 50, 50);
        // Higher rated player beats lower rated player (expected)
        int[] expectedRatings = eloService.calculateNewRatings(1200, 800, 50, 50);

        int upsetGain = upsetRatings[0] - 800;
        int expectedGain = expectedRatings[0] - 1200;

        assertTrue(upsetGain > expectedGain, "Upset win should give more rating gain");
    }

    @Test
    @DisplayName("New players have higher K-factor")
    void newPlayersHigherKFactor() {
        int newPlayerK = eloService.getKFactor(5);
        int establishedPlayerK = eloService.getKFactor(50);
        int experiencedPlayerK = eloService.getKFactor(150);

        assertTrue(newPlayerK > establishedPlayerK);
        assertTrue(establishedPlayerK > experiencedPlayerK);
    }

    @Test
    @DisplayName("Loser rating doesn't go below minimum")
    void loserRatingMinimum() {
        // Extreme case: very low rated player loses
        int[] newRatings = eloService.calculateNewRatings(1500, 100, 50, 50);

        assertTrue(newRatings[1] >= 100, "Loser rating should not go below minimum");
    }
}
