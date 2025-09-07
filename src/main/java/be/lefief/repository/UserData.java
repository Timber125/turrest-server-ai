package be.lefief.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserData {
    private UUID id;
    private String name;
    private LocalDateTime registeredAt;
    public UserData(UUID id, String name, LocalDateTime registeredAt) {
        this.id = id;
        this.name = name;
        this.registeredAt = registeredAt;
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

}
