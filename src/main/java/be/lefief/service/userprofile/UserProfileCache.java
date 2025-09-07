package be.lefief.service.userprofile;

import be.lefief.repository.UserData;
import be.lefief.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileCache {
    public static final String USER_PROFILE_BY_ID = "USERPROFILE_BY_ID";
    public static final String USER_PROFILE_BY_NAME = "USERPROFILE_BY_NAME";
    private static final Logger LOG = LoggerFactory.getLogger(UserProfileCache.class);
    private final CacheManager cacheManager;
    public UserProfileCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    public void update(UserData updated){
        updateCache(USER_PROFILE_BY_ID, updated.getId(), updated);
        updateCache(USER_PROFILE_BY_NAME, updated.getName(), updated);
    }

    private <T> void updateCache(String cacheName, T cacheKey, Object value){
        try {
            Optional.ofNullable(cacheManager.getCache(USER_PROFILE_BY_ID))
                    .ifPresent(cache -> cache.put(cacheKey, value));
        } catch (Exception e){
            LOG.error("Failed to update cache {} key {} with value {}", cacheName, cacheKey, value);
            LOG.error(e.getMessage(), e);
        }
    }
}
