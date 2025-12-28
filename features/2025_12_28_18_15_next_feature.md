Instructions:

- We've built a greate base game. Problem however is performance and latency issues.

Check the entire communication flow, make a plan to increase performance, mitigate latency and desync issues, and make sure we can keep building on top of this infrastructure to spawn way more creeps and send way more updates via serial commands.

---

# Performance Improvement Plan

## Current Architecture Analysis

### Tick System
- **Tick rate**: 1000ms (1 Hz) - defined in `TurrestGameMode01.java:41`
- Game loop calls `CreepManager.tick()` every second
- Resource production also triggered every tick

### Communication Flow
```
Backend Game Loop (1 Hz)
    ↓
CreepManager.tick()
    ↓
For each creep: broadcastToAllPlayers(UpdateCreepCommand)  ← BOTTLENECK
    ↓
Game.broadcastToAllPlayers() loops through all players
    ↓
SocketHandler.sendCommand() → JSON serialize → thread executor → socket
    ↓
Frontend WebSocket receives → JSON.parse → update state
    ↓
Animation loop interpolates positions (60 FPS)
```

### Identified Bottlenecks

| Bottleneck | Severity | Impact |
|------------|----------|--------|
| One command per creep per tick | **HIGH** | 50 creeps × 4 players = 200 commands/tick |
| No command batching | **HIGH** | JSON overhead per message |
| Resource updates every tick | MEDIUM | Unnecessary traffic when unchanged |
| Unused message queue | MEDIUM | Dead code in SocketHandler |
| Per-player broadcast loop | LOW | Context switching overhead |

### Current Traffic Estimates
- 10 creeps, 2 players: ~1.8 KB/sec
- 50 creeps, 4 players: **~16 KB/sec** (will cause lag)

---

## Implementation Plan

### Phase 1: Command Batching (High Impact)

**1.1 Create BatchedCreepUpdateCommand**

Location: `src/main/java/be/lefief/game/turrest01/commands/`

```java
public class BatchedCreepUpdateCommand extends ServerToClientCommand {
    private List<CreepUpdate> updates;  // [{id, x, y, hp}, ...]
}
```

**1.2 Modify CreepManager.moveCreeps()**

Location: `CreepManager.java:88-95`

Change from:
```java
for (Creep creep : activeCreeps.values()) {
    creep.move(1.0);
    game.broadcastToAllPlayers(new UpdateCreepCommand(creep));  // BAD
}
```

To:
```java
List<CreepUpdate> updates = new ArrayList<>();
for (Creep creep : activeCreeps.values()) {
    creep.move(1.0);
    updates.add(new CreepUpdate(creep));
}
game.broadcastToAllPlayers(new BatchedCreepUpdateCommand(updates));  // GOOD
```

**1.3 Create BatchedSpawnCreepCommand**

Same pattern for spawn phase - batch all spawns into single command per tick.

**1.4 Frontend Handler**

Location: `game.component.ts`

Add handler for `BATCHED_CREEP_UPDATE`:
```typescript
const batchSub = this.socketService.onCommand('GAME', 'BATCHED_CREEP_UPDATE')
  .subscribe(cmd => {
    for (const update of cmd.data.updates) {
      const creep = this.creeps.get(update.id);
      if (creep) {
        creep.targetX = update.x;
        creep.targetY = update.y;
        creep.hitpoints = update.hp;
      }
    }
  });
```

**Expected Impact**: Reduce 50 commands → 1 command per tick (~98% reduction)

---

### Phase 2: Tick Rate Optimization

**2.1 Increase Server Tick Rate to 200ms (5 Hz)**

Location: `TurrestGameMode01.java:41`

```java
private static final long TICK_RATE_MS = 200;  // Was 1000
```

**2.2 Adjust Creep Movement**

Location: `CreepManager.java:91`

```java
creep.move(0.2);  // deltaTime = 0.2 seconds per tick
```

**2.3 Throttle Resource Updates**

Only send resource updates when values change or every 5 ticks:
```java
private int resourceUpdateCounter = 0;

// In gameTick():
resourceUpdateCounter++;
if (resourceUpdateCounter >= 5 || resourcesChanged) {
    sendResourceUpdates();
    resourceUpdateCounter = 0;
}
```

**Expected Impact**: Smoother creep movement, reduced interpolation distance

---

### Phase 3: Delta Compression (Medium Impact)

**3.1 Track Previous State**

Only send creep updates when position changed significantly:

```java
public class Creep {
    private double lastSentX, lastSentY;

    public boolean hasMovedSignificantly() {
        double dx = Math.abs(x - lastSentX);
        double dy = Math.abs(y - lastSentY);
        return dx > 0.01 || dy > 0.01;
    }

    public void markSent() {
        lastSentX = x;
        lastSentY = y;
    }
}
```

**3.2 Filter Updates**

```java
List<CreepUpdate> updates = activeCreeps.values().stream()
    .filter(Creep::hasMovedSignificantly)
    .peek(Creep::markSent)
    .map(CreepUpdate::new)
    .collect(Collectors.toList());
```

**Expected Impact**: ~30% reduction in update frequency for stationary/slow creeps

---

### Phase 4: Message Queue Utilization

**4.1 Activate Existing Queue**

Location: `SocketHandler.java:32` - queue exists but unused

Implement queue flushing:
```java
public void flushMessages() {
    List<ServerToClientCommand> batch = new ArrayList<>();
    messageQueue.drainTo(batch);
    if (!batch.isEmpty()) {
        String combined = CommandSerializer.serializeBatch(batch);
        sendRaw(combined);
    }
}
```

**4.2 Call Flush at End of Tick**

```java
// In gameTick():
creepManager.tick(currentTick, this);
produceResources();
flushAllPlayerQueues();  // Send all batched commands
```

---

### Phase 5: Frontend Optimizations

**5.1 Reduce Change Detection**

Move creep rendering outside Angular zone:
```typescript
this.ngZone.runOutsideAngular(() => {
    this.startAnimationLoop();
});
```

**5.2 Object Pooling for Creeps**

Reuse creep objects instead of creating new ones:
```typescript
private creepPool: Creep[] = [];

private getCreep(): Creep {
    return this.creepPool.pop() || new Creep();
}

private releaseCreep(creep: Creep): void {
    this.creepPool.push(creep);
}
```

---

## Implementation Order

| Phase | Task | Estimated Effort | Impact |
|-------|------|-----------------|--------|
| 1.1 | BatchedCreepUpdateCommand | 1 hour | HIGH |
| 1.2 | Modify CreepManager | 30 min | HIGH |
| 1.4 | Frontend batch handler | 30 min | HIGH |
| 2.1 | Tick rate to 200ms | 15 min | MEDIUM |
| 2.3 | Throttle resource updates | 30 min | LOW |
| 3.1-3.2 | Delta compression | 1 hour | MEDIUM |
| 5.1 | NgZone optimization | 15 min | LOW |

**Total: ~4 hours for major improvements**

---

## Scalability Targets After Implementation

| Metric | Current | Target |
|--------|---------|--------|
| Max creeps | ~30 | 200+ |
| Max players | 2-3 | 8+ |
| Commands/tick | 50+ | 1-3 |
| Tick rate | 1 Hz | 5 Hz |
| Network/sec | 16 KB | <5 KB |ontend

---

## Quick Wins (Can Do Immediately)

1. **Batch creep updates** - Single biggest improvement
2. **Throttle resource updates** - Only send on change
3. **Remove unused scoreboard broadcasts** - Called too frequently in `checkCastleReached()`

---

## Implementation Status: COMPLETED

**Date:** 2025-12-28

### Changes Made:

**Backend:**
1. `BatchedCreepUpdateCommand.java` - New batched command for creep position updates
2. `BatchedSpawnCreepCommand.java` - New batched command for creep spawns
3. `CreepManager.java` - Modified to collect and batch all updates into single commands
4. `TurrestGameMode01.java`:
   - Tick rate changed from 1000ms → 200ms (5 Hz)
   - Resource updates throttled to every 5 ticks (1 second)
   - Passes deltaTime to CreepManager for smooth movement

**Frontend:**
1. `game.component.ts`:
   - Added `handleBatchedCreepUpdate()` handler
   - Added `handleBatchedSpawnCreep()` handler
   - Animation loop moved outside NgZone for better performance

### Performance Improvements:
| Metric | Before | After |
|--------|--------|-------|
| Commands/tick (50 creeps) | 50 | 1 |
| Tick rate | 1 Hz | 5 Hz |
| Resource updates | Every tick | Every 5 ticks |
| Angular change detection | Every frame | Only when needed |

