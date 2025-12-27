package be.lefief.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FakeKeycloak {

    private static final Logger LOG = LoggerFactory.getLogger(FakeKeycloak.class);
    private final Map<String, UUID> accessTokenMap;
    private final Timer timer;
    public FakeKeycloak(){
        accessTokenMap = new HashMap<>();
        timer = new Timer();
    }

    public String createAccessToken(UUID clientId){
        UUID token = UUID.randomUUID();
        String accessToken = "temptoken_" + token.toString();
        accessTokenMap.put(accessToken, clientId);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                accessTokenMap.remove(accessToken);
            }
        }, 4 * 60 * 60 * 1000L); // 4 hours
        return accessToken;
    }
    public boolean useAccessToken(String accessToken, UUID clientId){
        if(clientId == null) return false;
        // Validate token without removing - tokens expire after 4 hours
        if(accessTokenMap.containsKey(accessToken) && accessTokenMap.get(accessToken).toString().equals(clientId.toString())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invalidate a token (e.g., on logout).
     */
    public void invalidateToken(String accessToken) {
        accessTokenMap.remove(accessToken);
    }

}
