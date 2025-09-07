package be.lefief.service.userprofile;

import be.lefief.controller.dto.AuthenticationDTO;
import be.lefief.repository.UserData;
import be.lefief.repository.UserProfile;
import be.lefief.repository.UserProfileRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {
    public static final String USER_PROFILE_BY_ID = "USERPROFILE_BY_ID";
    public static final String USER_PROFILE_BY_NAME = "USERPROFILE_BY_NAME";
    private final UserProfileRepository userProfileRepository;
    private final UserProfileCache userProfileCache;
    public UserProfileService(
            UserProfileRepository userProfileRepository,
            UserProfileCache userProfileCache
    ){
        this.userProfileRepository = userProfileRepository;
        this.userProfileCache = userProfileCache;
    }

    public Optional<UserData> save(AuthenticationDTO authenticationModel) {
        LocalDateTime registeredAt = LocalDateTime.now();
        UUID id = UUID.randomUUID();
        UserProfile registeredProfile = new UserProfile(
                id,
                authenticationModel.username(),
                registeredAt,
                authenticationModel.password()
        );
        int success = userProfileRepository.save(registeredProfile);
        if(success == 1)
            return Optional.ofNullable(registeredProfile.getUserData());
        else
            return Optional.empty();
    }

    public UserData update(UUID id, String username){
        UserData existing = findByID(id).orElseThrow(() -> new EntityNotFoundException("No existing profile with id " + id.toString()));
        existing.setName(username);
        int success = userProfileRepository.update(existing);
        if(success == 1){
            userProfileCache.update(existing);
            return existing;
        } else {
            return null;
        }
    }

    @Cacheable(cacheNames = USER_PROFILE_BY_ID)
    public Optional<UserData> findByID(UUID id){
        return userProfileRepository.findByID(id);
    }

    @Cacheable(cacheNames = USER_PROFILE_BY_NAME)
    public Optional<UserData> findByUsername(String username){
        return userProfileRepository.findByName(username);
    }

    public Optional<UserData> login(String username, String password){
        return userProfileRepository.authenticate(username, password);
    }

}
