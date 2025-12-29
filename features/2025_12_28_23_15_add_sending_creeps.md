Instructions: 

- We're going to add another concept: sending creeps. 
- Actions for sending certain types of creeps will be shown in a seperate pane in the bottom bar -> we're going from [20%|60%|20%] to [20%|50%|20%|10%] effectively reducing the minimap size to make room for the send creeps pane. 
- Creeps will get a "sendCost" parameter - the cost to send this creep
- As we are going to use the concepts "cost" and "reward" a lot in coming features, i'd like you to abstract off for each type of game what a Reward and Cost can consist of. For turrest, a reward can consist of (goldAmount, woodAmount, stoneAmount, hitpointsAmount) and a cost can consist of (goldAmount, woodAmount, stoneAmount, hitpointsAmount). -> this forsees future "vampiric" costs where player pays with life for certain things, and forsees future rewards where HP is gained.

GHOST: 
- sendCost = (0 wood, 0 stone, 10 gold, 0 hitpoints)

TROLL:
- sendCost = (0 wood, 0 stone, 30 gold, 0 hitpoints)

Each type of creep will get its own "send creep" action for a certain Cost.
If player 1 uses the sendCreep functionality to send a ghost - all other players than player 1 will receive a spawned ghost in their spawners, 'sent by' by player 1.
If i recall correctly, we still have code lingering that will show player color around the sprite indicating which player this creep has been sent by (for wave spawns, this is null - and this is good)

---

## Implementation Plan

### Phase 1: Abstract Cost and Reward System

**1.1 Create `TurrestCost` class** (`src/main/java/be/lefief/game/turrest01/resource/TurrestCost.java`)
- Fields: `wood`, `stone`, `gold`, `hitpoints` (all int)
- Method: `canAfford(PlayerResources resources, int playerHitpoints)`
- Method: `apply(PlayerResources resources, Turrest01Player player)` - subtracts cost
- This replaces/extends the existing `ResourceCost` class

**1.2 Create `TurrestReward` class** (`src/main/java/be/lefief/game/turrest01/resource/TurrestReward.java`)
- Fields: `wood`, `stone`, `gold`, `hitpoints` (all int)
- Method: `apply(PlayerResources resources, Turrest01Player player)` - adds reward

**1.3 Update `CreepType` enum**
- Replace `goldReward` with `TurrestReward killReward`
- Add `TurrestCost sendCost` field
- GHOST: sendCost=(0,0,10,0), killReward=(0,0,5,0)
- TROLL: sendCost=(0,0,30,0), killReward=(0,0,15,0)

**1.4 Update existing code to use new abstractions**
- Update `CreepManager` to use `killReward.apply()` instead of `awardGoldToPlayer()`
- Update `TurrestGameMode01.awardGoldToPlayer()` to use reward system or keep as convenience method

### Phase 2: Backend Send Creep Logic

**2.1 Create `SendCreepCommand`** (`src/main/java/be/lefief/game/turrest01/commands/SendCreepCommand.java`)
- Client-to-server command
- Fields: `creepTypeId` (String)

**2.2 Create `SendCreepHandler` and `SendCreepListener`**
- Validates player can afford the send cost
- Subtracts cost from sending player
- Spawns creep for ALL OTHER players (not the sender)
- Creep's `spawnedByPlayer` = sender's player number

**2.3 Update `CreepManager`**
- Add method `spawnSentCreep(CreepType type, int senderPlayerNumber, TurrestGameMode01 game)`
- Spawns one creep per opponent at their spawner positions
- Sets `spawnedByPlayer` to sender (for colored contour display)

### Phase 3: Frontend Send Creeps Pane

**3.1 Create `SendCreepsPanelComponent`** (`frontend/src/app/features/game/components/send-creeps-panel/`)
- Similar structure to action-panel but for sending creeps
- Display creep types with icons and send costs
- Emit `creepSent` event with creep type

**3.2 Update `game.model.ts`**
- Add `CreepDefinition` interface with `id`, `name`, `icon`, `sendCost`
- Add `CREEP_DEFINITIONS` constant array

**3.3 Update bottom pane layout in `game.component.ts`**
- Change from `[20%|60%|20%]` to `[20%|50%|10%|20%]`
  - Tile Info: 20%
  - Actions: 50% (reduced from 60%)
  - Send Creeps: 10% (new)
  - Minimap: 20% (unchanged)
- Add `<app-send-creeps-panel>` component
- Handle `(creepSent)` event to send command to server

**3.4 Update `game.component.ts` handler**
- Add `onCreepSent(creepType: CreepDefinition)` method
- Send `SEND_CREEP` command via socket service

### Phase 4: Integration & Polish

**4.1 Wire up socket command**
- Register `SEND_CREEP` topic in socket handlers

**4.2 Visual feedback**
- Show error toast if player can't afford
- Existing colored contour on creeps already handles "sent by" display

### File Changes Summary

**New Files:**
- `src/main/java/be/lefief/game/turrest01/resource/TurrestCost.java`
- `src/main/java/be/lefief/game/turrest01/resource/TurrestReward.java`
- `src/main/java/be/lefief/game/turrest01/commands/SendCreepCommand.java`
- `src/main/java/be/lefief/game/turrest01/handlers/SendCreepHandler.java`
- `src/main/java/be/lefief/game/turrest01/handlers/SendCreepListener.java`
- `frontend/src/app/features/game/components/send-creeps-panel/send-creeps-panel.component.ts`

**Modified Files:**
- `src/main/java/be/lefief/game/turrest01/creep/CreepType.java` - add sendCost, use TurrestReward
- `src/main/java/be/lefief/game/turrest01/creep/CreepManager.java` - add spawnSentCreep method
- `src/main/java/be/lefief/game/turrest01/handlers/Turrest01GameHandler.java` - add handleSendCreep
- `frontend/src/app/shared/models/game.model.ts` - add CreepDefinition
- `frontend/src/app/features/game/game.component.ts` - layout change, add send panel, handler
