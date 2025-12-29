package be.lefief.sockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter for WebSocket commands to prevent abuse.
 * Uses a sliding window approach to track command counts per user.
 */
@Component
public class RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);

    // Maximum commands per window
    private static final int MAX_COMMANDS_PER_WINDOW = 50;
    // Window size in milliseconds (1 second)
    private static final long WINDOW_SIZE_MS = 1000;
    // Warning threshold (80% of max)
    private static final int WARNING_THRESHOLD = (int) (MAX_COMMANDS_PER_WINDOW * 0.8);

    private final Map<UUID, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check if a command should be allowed for the given user.
     *
     * @param userId The user's ID
     * @return true if the command is allowed, false if rate limited
     */
    public boolean allowCommand(UUID userId) {
        if (userId == null) {
            return true; // Allow unauthenticated commands (login, etc.)
        }

        RateLimitBucket bucket = buckets.computeIfAbsent(userId, k -> new RateLimitBucket());
        return bucket.tryAcquire();
    }

    /**
     * Get current command count for a user (for debugging/monitoring).
     */
    public int getCurrentCount(UUID userId) {
        RateLimitBucket bucket = buckets.get(userId);
        return bucket != null ? bucket.getCount() : 0;
    }

    /**
     * Remove rate limit tracking for a user (on disconnect).
     */
    public void removeUser(UUID userId) {
        buckets.remove(userId);
    }

    /**
     * Sliding window rate limit bucket.
     */
    private static class RateLimitBucket {
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile boolean warned = false;

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long currentWindowStart = windowStart.get();

            // Check if we need to start a new window
            if (now - currentWindowStart >= WINDOW_SIZE_MS) {
                // Reset the window
                if (windowStart.compareAndSet(currentWindowStart, now)) {
                    count.set(1);
                    warned = false;
                    return true;
                }
                // Another thread reset the window, retry
                return tryAcquire();
            }

            // Increment and check
            int currentCount = count.incrementAndGet();

            if (currentCount > MAX_COMMANDS_PER_WINDOW) {
                LOG.warn("Rate limit exceeded: {} commands in window", currentCount);
                return false;
            }

            if (currentCount >= WARNING_THRESHOLD && !warned) {
                warned = true;
                LOG.debug("Rate limit warning: {} commands approaching limit", currentCount);
            }

            return true;
        }

        int getCount() {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() >= WINDOW_SIZE_MS) {
                return 0;
            }
            return count.get();
        }
    }
}
