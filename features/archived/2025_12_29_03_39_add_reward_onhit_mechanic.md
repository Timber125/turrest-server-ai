Instructions:

- We have a Cost and Reward abstract system in place - So now i want you to add a "reward" when a player-sent creep hits an enemy castle.
- For starters, when the creep hits the castle, the reward will be {sendCost} + 5 gold.
- This reward applies for each castle hit - so if 3 players are playing you can get double reward etc.

---

## Implementation Plan

### Overview

Currently when a creep reaches a castle:
1. `CreepManager.checkCastleReached()` detects the creep reached the castle
2. Damage is applied to the castle owner
3. The creep is removed

**Missing:** When a player-sent creep (with `spawnedByPlayer != null`) hits a castle, the sender receives no reward.

**Goal:** Award the sender `sendCost.gold + 5` gold for each enemy castle hit.

---

### Current Code Flow

**`CreepManager.checkCastleReached()` (simplified):**
```java
for (Creep creep : creeps.values()) {
    if (creep.hasReachedCastle()) {
        // Deal damage to castle owner
        Turrest01Player owner = game.getPlayerByNumber().get(creep.getOwnerPlayerNumber());
        owner.takeDamage(creep.getType().getDamage());

        // Remove creep
        creeps.remove(creep.getId());
        despawnedCreepIds.add(creep.getId());
    }
}
```

**Key fields on Creep:**
- `Integer spawnedByPlayer` - null for wave creeps, player number for sent creeps
- `CreepType type` - has `getSendCost()` returning `TurrestCost` with gold amount
- `int ownerPlayerNumber` - which player's section the creep is attacking

---

### Implementation Steps

#### Step 1: Add Hit Reward to CreepType

**File: `src/main/java/be/lefief/game/turrest01/creep/CreepType.java`**

Add a method to calculate the hit reward:

```java
public TurrestReward getHitReward() {
    // sendCost.gold + 5 bonus
    return TurrestReward.gold(sendCost.getGold() + 5);
}
```

This keeps the reward calculation centralized in CreepType where sendCost is defined.

**Current CreepType values:**
- GHOST: sendCost = 10 gold → hitReward = 15 gold
- TROLL: sendCost = 30 gold → hitReward = 35 gold

#### Step 2: Award Reward in CreepManager.checkCastleReached()

**File: `src/main/java/be/lefief/game/turrest01/creep/CreepManager.java`**

In the `checkCastleReached()` method, after handling the damage, add reward logic:

```java
// After dealing damage to castle owner...

// Award reward to sender if this was a player-sent creep
Integer senderPlayerNumber = creep.getSpawnedByPlayer();
if (senderPlayerNumber != null) {
    Turrest01Player sender = game.getPlayerByNumber().get(senderPlayerNumber);
    if (sender != null && sender.isAlive()) {
        TurrestReward hitReward = creep.getType().getHitReward();
        hitReward.apply(sender);
        playersNeedingResourceUpdate.add(sender);
    }
}
```

#### Step 3: Batch Resource Updates for Rewarded Players

**File: `src/main/java/be/lefief/game/turrest01/creep/CreepManager.java`**

The method already tracks players needing updates (for damage). Extend this to include rewarded players:

```java
private void checkCastleReached(TurrestGameMode01 game) {
    Set<Turrest01Player> playersNeedingResourceUpdate = new HashSet<>();
    List<UUID> despawnedCreepIds = new ArrayList<>();

    for (Creep creep : creeps.values()) {
        if (creep.hasReachedCastle()) {
            // 1. Deal damage to castle owner
            Turrest01Player owner = game.getPlayerByNumber().get(creep.getOwnerPlayerNumber());
            owner.takeDamage(creep.getType().getDamage());
            playersNeedingResourceUpdate.add(owner); // for HP update

            // 2. Award reward to sender (if player-sent)
            Integer senderPlayerNumber = creep.getSpawnedByPlayer();
            if (senderPlayerNumber != null) {
                Turrest01Player sender = game.getPlayerByNumber().get(senderPlayerNumber);
                if (sender != null && sender.isAlive()) {
                    creep.getType().getHitReward().apply(sender);
                    playersNeedingResourceUpdate.add(sender);
                }
            }

            // 3. Remove creep
            creeps.remove(creep.getId());
            despawnedCreepIds.add(creep.getId());
        }
    }

    // Send resource updates to all affected players
    for (Turrest01Player player : playersNeedingResourceUpdate) {
        game.sendResourceUpdate(player);
    }

    // Broadcast despawns...
}
```

#### Step 4: Ensure TurrestCost Has Gold Getter

**File: `src/main/java/be/lefief/game/turrest01/resource/TurrestCost.java`**

Verify or add a `getGold()` method:

```java
public int getGold() {
    return gold;
}
```

---

### Files to Modify

| File | Changes |
|------|---------|
| `CreepType.java` | Add `getHitReward()` method |
| `TurrestCost.java` | Ensure `getGold()` getter exists |
| `CreepManager.java` | Add reward logic in `checkCastleReached()` |

---

### Testing Considerations

**Unit Tests to Add:**

1. **CreepTypeTest**
   - `testGetHitReward_Ghost()` - verify GHOST returns 15 gold reward
   - `testGetHitReward_Troll()` - verify TROLL returns 35 gold reward

2. **CreepManagerTest**
   - `testCheckCastleReached_playerSentCreep_awardsReward()` - sender gets gold
   - `testCheckCastleReached_waveCreep_noReward()` - no reward for wave creeps
   - `testCheckCastleReached_multipleOpponents_multipleRewards()` - 2 castles = 2 rewards
   - `testCheckCastleReached_senderDead_noReward()` - dead senders don't get rewards

**Manual Testing Scenarios:**

1. Send a GHOST creep → expect +15 gold when it hits enemy castle
2. Send a TROLL creep → expect +35 gold when it hits enemy castle
3. In 3-player game, send creep → expect reward for each of the 2 castles hit
4. Die before your creep hits → verify no reward granted

---

### Edge Cases to Handle

1. **Sender disconnected/dead:** Check `sender.isAlive()` before applying reward
2. **Wave creeps:** `spawnedByPlayer == null`, skip reward logic
3. **Multiple castles hit same tick:** Each hit is processed independently, sender gets multiple rewards
4. **Resource updates:** Batch updates to avoid sending multiple messages per tick

---

### Reward Formula Reference

| Creep Type | Send Cost (Gold) | Hit Reward (Gold) |
|------------|------------------|-------------------|
| GHOST      | 10               | 15 (10 + 5)       |
| TROLL      | 30               | 35 (30 + 5)       |

The +5 bonus makes sending creeps profitable when they successfully hit castles, incentivizing aggressive play.

---

### Future Extensibility

The implementation supports easy extension:
- Change bonus amount by modifying `getHitReward()` formula
- Add wood/stone rewards by returning a full `TurrestReward` object
- Add hitpoint healing on castle hit if desired
- Per-creep-type bonuses can be configured in the enum
