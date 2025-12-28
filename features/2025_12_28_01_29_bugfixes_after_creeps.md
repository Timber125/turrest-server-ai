in [2025_12_27_23_53_creeps.md](2025_12_27_23_53_creeps.md) we added creeps ;

Job well done!

However, few old bugs are back...

1. Session management is still a pain. Lets stop doing session management, and have each tab have its own shortlived memory.
2. If an already logged in user logs in, his current token should get revoked & the last granted token should be valid - so that the newly logged in client "kicks" the already logged in client.
3. One of the players couldn't build again... this is a huge bug, please get to the bottom of it
4. Frontend should make the creeps transition seemlessly instead of in bumps per tick

---

# Implementation Plan

## Issue 1: Simplify Session Management

### Problem
Current session management is complex with `userId:tabId` composite keys, multi-tab collision risks, and inconsistent handling across LobbyService, GameService, and ConnectionHandler.

### Solution: One Session Per User (Last Login Wins)

Remove tabId complexity. Each user has exactly ONE active session at a time. If they open a new tab or re-login, the old session is disconnected.

### Backend Changes

**1. FakeKeycloak.java - Single Token Per User**
```java
// Replace accessTokenMap with userId->token mapping
private final Map<UUID, String> userActiveToken = new ConcurrentHashMap<>();

public String createAccessToken(UUID userId) {
    // Invalidate any existing token for this user
    String existingToken = userActiveToken.get(userId);
    if (existingToken != null) {
        accessTokenMap.remove(existingToken);
    }

    String token = "temptoken_" + UUID.randomUUID();
    accessTokenMap.put(token, userId);
    userActiveToken.put(userId, token);
    return token;
}
```

**2. LobbyService.java - Simplify to userId Keys**
```java
// Change from Map<String, ClientSession> (userId:tabId) to:
private final Map<UUID, ClientSession> identifiedClients = new ConcurrentHashMap<>();

// Remove getSessionKey() method entirely
// Use userId directly as the key
```

**3. GameService.java - Simplify to userId Keys**
```java
// Change from Map<String, UUID> to:
private final Map<UUID, UUID> playerActiveGame = new ConcurrentHashMap<>();
```

**4. ConnectionHandler.java - Kick Old Session on Login**
```java
// When authenticating:
ClientSession existingSession = lobbyService.getClientSession(userId);
if (existingSession != null && existingSession != clientSession) {
    // Send "kicked" message to old session
    existingSession.sendCommand(new KickedCommand("Logged in from another location"));
    existingSession.disconnect();
    lobbyService.removeClient(userId);
}
```

**5. Remove tabId from ClientSession**
- Remove `getTabId()` / `setTabId()` methods
- Remove tabId parameter from ConnectionCommand

### Frontend Changes

**1. Remove tabId from auth.service.ts**
```typescript
// Remove sessionStorage tabId generation/storage
// Remove tabId from login payload
```

**2. Handle KickedCommand in socket.service.ts**
```typescript
// Listen for KICKED command
// Show message "You were logged out because you logged in from another location"
// Redirect to login
```

### Files to Modify
- `FakeKeycloak.java` - Single token per user
- `LobbyService.java` - Remove tabId from keys
- `GameService.java` - Remove tabId from keys
- `ConnectionHandler.java` - Kick old session logic
- `ClientSession.java` / `WebSocketClientSession.java` - Remove tabId
- `ConnectionCommand.java` - Remove tabId field
- `frontend/auth.service.ts` - Remove tabId
- `frontend/socket.service.ts` - Handle kicked command

---

## Issue 2: Token Revocation on Re-Login

This is solved by Issue 1's implementation. When a user logs in:
1. `FakeKeycloak.createAccessToken()` invalidates their old token
2. `ConnectionHandler` kicks the old WebSocket session
3. Old client receives KICKED command and redirects to login

---

## Issue 3: Building Placement Bug Investigation

### Likely Causes

Based on previous investigation, the building placement bug was caused by:
1. Session key mismatch between lobby and game
2. `findPlayerBySession()` matching too strictly

### Investigation Steps

1. Add detailed logging to trace the flow:
```java
// In Turrest01GameHandler.handlePlaceBuilding()
LOG.info("PLACE_BUILDING from session: clientId={}", session.getClientID());
LOG.info("Game players: {}", game.getPlayerByNumber().keySet());
for (Turrest01Player p : game.getPlayerByNumber().values()) {
    LOG.info("  Player {}: clientId={}", p.getPlayerNumber(),
        p.getClientSession() != null ? p.getClientSession().getClientID() : "null");
}
```

2. Check if `findPlayerBySession()` returns null:
```java
Turrest01Player player = findPlayerBySession(game, session);
if (player == null) {
    LOG.error("Could not find player for session clientId={}", session.getClientID());
    // List all player sessions for debugging
}
```

3. Verify player numbers are assigned correctly during game start

### Potential Fix

If the issue persists after Issue 1 simplification, ensure:
- `Game.createPlayer()` correctly stores the ClientSession
- Session references are not stale after reconnection
- Player number assignment matches territory ownership

---

## Issue 4: Smooth Creep Animation

### Problem
Creeps move in 1-second "bumps" because the frontend only updates position when UPDATE_CREEP arrives (once per tick).

### Solution: Client-Side Interpolation

Frontend should interpolate creep positions between ticks for smooth movement.

### Implementation

**1. Store velocity and target position**
```typescript
export interface Creep {
    id: string;
    creepType: string;
    x: number;           // Current rendered position
    y: number;
    targetX: number;     // Server-sent position (destination)
    targetY: number;
    playerNumber: number;
    hitpoints: number;
    maxHitpoints: number;
    speed: number;       // tiles per second (10/creepSpeed)
}
```

**2. Update handleUpdateCreep to set target**
```typescript
private handleUpdateCreep(data: Record<string, any>): void {
    const creepId = data['creepId'] as string;
    const creep = this.creeps.get(creepId);
    if (creep) {
        // Set target position, don't jump immediately
        creep.targetX = data['x'] as number;
        creep.targetY = data['y'] as number;
        creep.hitpoints = data['hitpoints'] as number;
    }
}
```

**3. Add animation loop**
```typescript
private lastFrameTime = 0;

ngAfterViewInit(): void {
    // ... existing code
    this.startAnimationLoop();
}

private startAnimationLoop(): void {
    const animate = (timestamp: number) => {
        const deltaTime = (timestamp - this.lastFrameTime) / 1000; // seconds
        this.lastFrameTime = timestamp;

        this.updateCreepPositions(deltaTime);
        this.render();

        requestAnimationFrame(animate);
    };
    requestAnimationFrame(animate);
}

private updateCreepPositions(deltaTime: number): void {
    for (const creep of this.creeps.values()) {
        const dx = creep.targetX - creep.x;
        const dy = creep.targetY - creep.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0.01) {
            const moveDistance = creep.speed * deltaTime;
            const ratio = Math.min(moveDistance / distance, 1);
            creep.x += dx * ratio;
            creep.y += dy * ratio;
        }
    }
}
```

**4. Send speed in SpawnCreepCommand**
```java
// SpawnCreepCommand.java
data.put("speed", creep.getType().getTilesPerSecond());
```

**5. Store speed on spawn**
```typescript
private handleSpawnCreep(data: Record<string, any>): void {
    const creep: Creep = {
        // ... existing fields
        speed: data['speed'] as number || 0.33, // Default: 10/30 for GHOST
        targetX: data['x'] as number,
        targetY: data['y'] as number,
    };
    // ...
}
```

### Files to Modify
- `frontend/src/app/shared/models/game.model.ts` - Add targetX, targetY, speed to Creep
- `frontend/src/app/features/game/game.component.ts` - Animation loop + interpolation
- `SpawnCreepCommand.java` - Include speed in data

---

## Implementation Order

1. **Issue 4: Smooth Creep Animation** (standalone, quick win)
2. **Issue 1 & 2: Session Simplification** (major refactor, fixes token issue)
3. **Issue 3: Building Bug** (investigate after session fix, may be resolved by it)

---

## Testing Checklist

- [ ] Creeps move smoothly between ticks
- [ ] Opening new tab kicks old tab with message
- [ ] Re-logging in kicks previous session
- [ ] Both players can place buildings
- [ ] Game works normally after session simplification
- [ ] Reconnection still works (refresh browser)