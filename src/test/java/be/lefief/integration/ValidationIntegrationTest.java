package be.lefief.integration;

import be.lefief.game.validation.CommandValidator;
import be.lefief.game.validation.ValidationResult;
import be.lefief.sockets.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the validation system.
 */
@DisplayName("Validation Integration Tests")
class ValidationIntegrationTest {

    @Test
    @DisplayName("Rate limiter allows normal traffic")
    void rateLimiterAllowsNormalTraffic() {
        RateLimiter rateLimiter = new RateLimiter();
        UUID userId = UUID.randomUUID();

        // Normal usage - should allow all commands
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiter.allowCommand(userId),
                    "Command " + i + " should be allowed");
        }
    }

    @Test
    @DisplayName("Rate limiter blocks excessive traffic")
    void rateLimiterBlocksExcessiveTraffic() {
        RateLimiter rateLimiter = new RateLimiter();
        UUID userId = UUID.randomUUID();

        // Exceed the limit (50 commands per second)
        int allowedCount = 0;
        int blockedCount = 0;

        for (int i = 0; i < 100; i++) {
            if (rateLimiter.allowCommand(userId)) {
                allowedCount++;
            } else {
                blockedCount++;
            }
        }

        assertTrue(allowedCount <= 50, "Should allow at most 50 commands");
        assertTrue(blockedCount > 0, "Should block some commands");
    }

    @Test
    @DisplayName("Rate limiter allows unauthenticated users")
    void rateLimiterAllowsUnauthenticated() {
        RateLimiter rateLimiter = new RateLimiter();

        // Null userId (unauthenticated) should always be allowed
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiter.allowCommand(null),
                    "Unauthenticated command should be allowed");
        }
    }

    @Test
    @DisplayName("ValidationResult correctly indicates success")
    void validationResultSuccess() {
        ValidationResult result = ValidationResult.success();

        assertTrue(result.isValid());
        assertFalse(result.isInvalid());
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("ValidationResult correctly indicates failure")
    void validationResultFailure() {
        ValidationResult result = ValidationResult.failure("Test error");

        assertFalse(result.isValid());
        assertTrue(result.isInvalid());
        assertEquals("Test error", result.getErrorMessage());
    }
}
