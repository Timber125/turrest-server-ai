# Implementation Plan for Proposals

Detailed implementation plan for features in proposals.md.

---

## Phase 1: Input Validation & Anti-Cheat (High Priority)

**Why first:** Foundation for fair gameplay. Low risk, high value. No architectural changes needed.

### Step 1.1: Server-Side Resource Validation
**Files to modify:**
- `src/main/java/be/lefief/game/turrest02/handlers/PlaceTowerHandler.java`
- `src/main/java/be/lefief/game/turrest02/handlers/PlaceBuildingHandler.java`
- `src/main/java/be/lefief/game/turrest02/handlers/SendCreepHandler.java`

**Implementation:**
1. Before processing placement, verify player has sufficient resources
2. Verify tile is valid (not occupied, within player's section, correct terrain)
3. Deduct resources only after validation passes
4. Return error response with reason on failure

**New file:**
- `src/main/java/be/lefief/game/validation/CommandValidator.java` - Centralized validation logic

### Step 1.2: Rate Limiting
**Files to create:**
- `src/main/java/be/lefief/sockets/RateLimiter.java`

**Implementation:**
1. Track command counts per session with sliding window (e.g., 100 commands/second max)
2. Apply in `SocketHandler.onMessage()` before routing
3. Disconnect abusive clients with warning

### Step 1.3: Timing Sanity Checks
**Files to modify:**
- `src/main/java/be/lefief/game/turrest02/Turrest02Player.java`

**Implementation:**
1. Track last action timestamps per player
2. Enforce minimum cooldowns between repeated actions
3. Log suspicious patterns for review

---

## Phase 2: Error Recovery & Reconnection (High Priority)

**Why second:** Improves player experience. Builds on validation work.

### Step 2.1: Disconnection Grace Period
**Files to modify:**
- `src/main/java/be/lefief/game/Game.java`
- `src/main/java/be/lefief/game/Player.java`
- `src/main/java/be/lefief/sockets/SocketHandler.java`

**Implementation:**
1. Add `disconnectedAt` timestamp to Player
2. On disconnect, mark player as disconnected instead of removing
3. Start 60-second grace period timer
4. Pause player's resource generation during disconnect
5. Remove player only after grace period expires

### Step 2.2: State Sync on Reconnection
**Files to modify:**
- `src/main/java/be/lefief/game/turrest02/TurrestGameMode02.java`
- `src/main/java/be/lefief/sockets/handlers/ConnectionHandler.java`

**New file:**
- `src/main/java/be/lefief/game/turrest02/commands/GameStateSnapshot.java`

**Implementation:**
1. On reconnect with valid session, check for in-progress game
2. Send full `GameStateSnapshot` with current: map state, all towers, active creeps, resources, HP
3. Resume player's game loop participation

### Step 2.3: Message Acknowledgment
**Files to modify:**
- `src/main/java/be/lefief/sockets/commands/server/ServerToClientCommand.java`

**Implementation:**
1. Add sequence numbers to critical commands
2. Track unacknowledged messages per session
3. Resend on reconnection if not acknowledged

---

## Phase 3: Game State Persistence (High Priority)

**Why third:** Enables replays and spectating. Requires stable game state model.

### Step 3.1: Game State Serialization
**Files to create:**
- `src/main/java/be/lefief/game/persistence/GameStateSerializer.java`
- `src/main/java/be/lefief/game/persistence/GameSnapshot.java`

**Implementation:**
1. Create serializable snapshot class containing full game state
2. Serialize to JSON using Jackson
3. Store snapshots at key moments (game start, every 30 seconds, game end)

### Step 3.2: Database Storage
**Files to create:**
- `src/main/resources/db/scripts/allenv/V3_xxxx__create_game_snapshots_table.sql`
- `src/main/java/be/lefief/repository/GameSnapshotRepository.java`

**Schema:**
```sql
CREATE TABLE game_snapshots (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    tick_number INT NOT NULL,
    snapshot_data TEXT NOT NULL,  -- JSON blob
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Step 3.3: Game Resume
**Files to modify:**
- `src/main/java/be/lefief/game/GameService.java`

**Implementation:**
1. Add `resumeGame(gameId)` method
2. Load latest snapshot from database
3. Reconstruct game state from snapshot
4. Notify reconnecting players

### Step 3.4: Replay System
**Files to create:**
- `src/main/java/be/lefief/game/replay/ReplayController.java`
- `src/main/java/be/lefief/game/replay/ReplayService.java`

**Implementation:**
1. Store all game commands with timestamps
2. Provide API to fetch replay data
3. Frontend replay player (playback commands in sequence)

---

## Phase 4: AI Opponents (Medium Priority)

**Why fourth:** Enables single-player. Good for testing. Depends on stable game mechanics.

### Step 4.1: Bot Player Interface
**Files to create:**
- `src/main/java/be/lefief/game/ai/BotPlayer.java`
- `src/main/java/be/lefief/game/ai/BotSession.java` (implements ClientSession)

**Implementation:**
1. Create BotPlayer extending Player with AI decision-making
2. BotSession that queues commands internally (no network)
3. Register bots as regular players in game

### Step 4.2: AI Decision Engine
**Files to create:**
- `src/main/java/be/lefief/game/ai/strategy/BotStrategy.java` (interface)
- `src/main/java/be/lefief/game/ai/strategy/EasyBotStrategy.java`
- `src/main/java/be/lefief/game/ai/strategy/MediumBotStrategy.java`
- `src/main/java/be/lefief/game/ai/strategy/HardBotStrategy.java`

**Implementation:**
- **Easy:** Random tower placement, random creep sending
- **Medium:** Prioritize blocking paths, balanced economy
- **Hard:** Optimal tower placement, counter enemy creep types, resource optimization

### Step 4.3: Single-Player Mode
**Files to modify:**
- `src/main/java/be/lefief/lobby/LobbyManager.java`
- Frontend: `lobby-room.component.ts`

**Implementation:**
1. Add "Add Bot" button in lobby
2. Support starting game with mix of humans and bots
3. Bots run on server tick cycle

---

## Phase 5: Player Matchmaking (Medium Priority)

**Why fifth:** Replaces lobby system. Requires player stats from Phase 3.

### Step 5.1: ELO Rating System
**Files to create:**
- `src/main/java/be/lefief/service/matchmaking/EloService.java`

**Database migration:**
```sql
ALTER TABLE player_stats ADD COLUMN elo_rating INT DEFAULT 1000;
```

**Implementation:**
1. Calculate ELO changes after each game
2. K-factor based on games played (higher for new players)
3. Update player stats with new rating

### Step 5.2: Matchmaking Queue
**Files to create:**
- `src/main/java/be/lefief/service/matchmaking/MatchmakingService.java`
- `src/main/java/be/lefief/service/matchmaking/MatchmakingQueue.java`
- `src/main/java/be/lefief/controller/MatchmakingController.java`

**Implementation:**
1. Players join queue with preferred mode (ranked/casual)
2. Matcher runs every 5 seconds, finds players within ELO range
3. Create lobby automatically when match found
4. Expand ELO range over time if no match

### Step 5.3: Frontend Integration
**Files to create:**
- `frontend/src/app/features/matchmaking/matchmaking.component.ts`
- `frontend/src/app/core/services/matchmaking.service.ts`

**Implementation:**
1. "Find Match" button on main menu
2. Show queue status, estimated wait time
3. Auto-join game when match found

---

## Phase 6: Observability (Low Priority)

### Step 6.1: Metrics with Micrometer
**Files to modify:**
- `pom.xml` - Add micrometer-registry-prometheus dependency
- `src/main/java/be/lefief/config/MetricsConfig.java`

**Metrics to track:**
- `games_active` - Current active games
- `players_online` - Connected players
- `commands_per_second` - Command throughput
- `game_duration_seconds` - Game length histogram
- `tick_processing_ms` - Game loop performance

### Step 6.2: Structured Logging
**Files to modify:**
- `src/main/resources/logback.xml`
- Key service classes

**Implementation:**
1. Add MDC context (gameId, playerId, sessionId)
2. JSON log format for production
3. Correlation IDs for request tracing

---

## Phase 7: Code Quality (Low Priority)

### Step 7.1: Integration Tests
**Files to create:**
- `src/test/java/be/lefief/integration/GameFlowIntegrationTest.java`
- `src/test/java/be/lefief/integration/WebSocketTestClient.java`

**Implementation:**
1. Test full game flow: connect → create lobby → start game → place tower → end game
2. Use embedded H2 database
3. Mock WebSocket client for assertions

### Step 7.2: Frontend Refactoring
**Files to modify:**
- `frontend/src/app/features/game/game.component.ts` (split into services)

**New files:**
- `frontend/src/app/core/services/game-state.service.ts`
- `frontend/src/app/core/services/game-renderer.service.ts`
- `frontend/src/app/core/services/game-input.service.ts`

**Implementation:**
1. Extract state management to GameStateService
2. Extract canvas rendering to GameRendererService
3. Extract input handling to GameInputService
4. Keep GameComponent as orchestrator (~500 lines max)

### Step 7.3: Remove Legacy Code
**Files to delete:**
- `src/main/java/be/lefief/controller/SocketController.java` (if unused)

**Verification:** Ensure no clients depend on TCP socket before removal.

---

## Dependency Graph

```
Phase 1 (Validation) ─────────────────────────────────────────────┐
                                                                  │
Phase 2 (Reconnection) ──────────────────────────────────────────┤
                                                                  │
Phase 3 (Persistence) ───────► Phase 5 (Matchmaking)             │
         │                                                        │
         └──────────────────► Phase 4 (AI) ──────────────────────┤
                                                                  │
Phase 6 (Observability) ──────────────────────────────────────────┤
                                                                  │
Phase 7 (Code Quality) ───────────────────────────────────────────┘
```

Phases 1-3 are sequential prerequisites. Phases 4-7 can be parallelized after Phase 3 completes.

---

## Estimated Scope

| Phase | Complexity | New Files | Modified Files |
|-------|------------|-----------|----------------|
| 1. Validation | Low | 2 | 4 |
| 2. Reconnection | Medium | 2 | 5 |
| 3. Persistence | Medium | 5 | 2 |
| 4. AI | High | 6 | 2 |
| 5. Matchmaking | High | 5 | 3 |
| 6. Observability | Low | 1 | 3 |
| 7. Code Quality | Medium | 5 | 2 |
