package be.lefief.game.turrest01.creep;

import be.lefief.game.turrest01.resource.TurrestReward;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreepType Tests")
class CreepTypeTest {

    @Test
    @DisplayName("GHOST hit reward is sendCost + 5 gold (15 gold)")
    void testGetHitReward_Ghost() {
        TurrestReward reward = CreepType.GHOST.getHitReward();

        assertEquals(15, reward.getGold());
        assertEquals(0, reward.getWood());
        assertEquals(0, reward.getStone());
        assertEquals(0, reward.getHitpoints());
    }

    @Test
    @DisplayName("TROLL hit reward is sendCost + 5 gold (35 gold)")
    void testGetHitReward_Troll() {
        TurrestReward reward = CreepType.TROLL.getHitReward();

        assertEquals(35, reward.getGold());
        assertEquals(0, reward.getWood());
        assertEquals(0, reward.getStone());
        assertEquals(0, reward.getHitpoints());
    }

    @Test
    @DisplayName("Hit reward is always sendCost.gold + 5")
    void testHitRewardFormula() {
        for (CreepType type : CreepType.values()) {
            int expectedGold = type.getSendCost().getGold() + 5;
            TurrestReward hitReward = type.getHitReward();

            assertEquals(expectedGold, hitReward.getGold(),
                    "Hit reward for " + type.getId() + " should be sendCost + 5");
        }
    }

    @Test
    @DisplayName("GHOST send cost is 10 gold")
    void testGhostSendCost() {
        assertEquals(10, CreepType.GHOST.getSendCost().getGold());
    }

    @Test
    @DisplayName("TROLL send cost is 30 gold")
    void testTrollSendCost() {
        assertEquals(30, CreepType.TROLL.getSendCost().getGold());
    }

    @Test
    @DisplayName("Kill reward is separate from hit reward")
    void testKillRewardVsHitReward() {
        // Kill reward (when creep is killed by towers)
        assertEquals(5, CreepType.GHOST.getKillReward().getGold());
        assertEquals(15, CreepType.TROLL.getKillReward().getGold());

        // Hit reward (when creep reaches castle) - should be different
        assertEquals(15, CreepType.GHOST.getHitReward().getGold());
        assertEquals(35, CreepType.TROLL.getHitReward().getGold());
    }

    @Test
    @DisplayName("fromId returns correct creep type")
    void testFromId() {
        assertEquals(CreepType.GHOST, CreepType.fromId("GHOST"));
        assertEquals(CreepType.TROLL, CreepType.fromId("TROLL"));
        assertEquals(CreepType.GHOST, CreepType.fromId("ghost")); // case insensitive
        assertNull(CreepType.fromId("INVALID"));
    }
}
