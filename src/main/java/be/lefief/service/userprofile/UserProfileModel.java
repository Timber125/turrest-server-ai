package be.lefief.service.userprofile;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserProfileModel {
    private UUID id;
    private String name;
    private LocalDateTime registeredAt;
    private String password;
    public UserProfileModel(UUID id, String name, LocalDateTime registeredAt, String password) {
        this.id = id;
        this.name = name;
        this.registeredAt = registeredAt;
        this.password = password;
    }
    public UserProfileModel() {}
    public UserProfileModel setId(UUID id) {
        this.id = id;
        return this;
    }
    public UserProfileModel setName(String name) {
        this.name = name;
        return this;
    }
    public UserProfileModel setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
        return this;
    }
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

}
