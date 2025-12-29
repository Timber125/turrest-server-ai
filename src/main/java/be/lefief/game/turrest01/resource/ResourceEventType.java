package be.lefief.game.turrest01.resource;

import lombok.Getter;

/**
 * Types of resource events for animation purposes.
 * Each type can have different visual/audio treatment on the frontend.
 */
@Getter
public enum ResourceEventType {
    // Rewards
    CREEP_KILL("kill"),           // Killed enemy creep with tower
    CREEP_HIT_CASTLE("hit"),      // Sent creep hit enemy castle

    // Costs
    BUILD_BUILDING("build"),      // Placed a building
    BUILD_TOWER("tower"),         // Placed a tower
    SEND_CREEP("send");           // Sent a creep to opponents

    private final String id;

    ResourceEventType(String id) {
        this.id = id;
    }
}
