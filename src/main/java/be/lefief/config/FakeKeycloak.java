package be.lefief.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FakeKeycloak {

    private static final Logger LOG = LoggerFactory.getLogger(FakeKeycloak.class);
    private final Map<String, UUID> accessTokenMap;
    private final Map<UUID, String> userActiveToken;  // userId -> their current active token
    private final Timer timer;

    public FakeKeycloak(){
        accessTokenMap = new HashMap<>();
        userActiveToken = new HashMap<>();
        timer = new Timer();
    }

    public String createAccessToken(UUID userId){
        // Invalidate any existing token for this user (single session per user)
        String existingToken = userActiveToken.get(userId);
        if (existingToken != null) {
            accessTokenMap.remove(existingToken);
            LOG.info("Invalidated existing token for user {}", userId);
        }

        UUID token = UUID.randomUUID();
        String accessToken = "temptoken_" + token.toString();
        accessTokenMap.put(accessToken, userId);
        userActiveToken.put(userId, accessToken);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                accessTokenMap.remove(accessToken);
                userActiveToken.remove(userId, accessToken);  // Only remove if still the active token
            }
        }, 4 * 60 * 60 * 1000L); // 4 hours
        return accessToken;
    }
    public boolean useAccessToken(String accessToken, UUID userId){
        if(userId == null) return false;
        // Validate token without removing - tokens expire after 4 hours
        if(accessTokenMap.containsKey(accessToken) && accessTokenMap.get(accessToken).toString().equals(userId.toString())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invalidate a token (e.g., on logout).
     */
    public void invalidateToken(String accessToken) {
        UUID userId = accessTokenMap.remove(accessToken);
        if (userId != null) {
            userActiveToken.remove(userId, accessToken);
        }
    }

}
