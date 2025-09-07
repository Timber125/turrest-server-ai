package be.lefief.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserProfile {
    private UUID id;
    private String name;
    private LocalDateTime registeredAt;
    private String password;
    public UserProfile(UUID id, String name, LocalDateTime registeredAt, String password) {
        this.id = id;
        this.name = name;
        this.registeredAt = registeredAt;
        this.password = password;
    }

    public void setName(String name){
        this.name = name;
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

    public String getPassword() {
        return password;
    }

    public UserData getUserData(){
        return new UserData(id, name, registeredAt);
    }
}
