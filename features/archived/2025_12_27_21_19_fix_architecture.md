Instruction:

- Gametick does an unneccesary "instanceof" check.
Solution: i want you to edit class `Game` so that class Game<T extends Player>.
Game is abstract, and each specific game will have a specific subtype of Player bound to it.
We will then have `TurrestGameMode01 extends Game<Turrest01Player>` and won't have to instanceof all the time.

- PlayerColor may be added to the general "Player".
It will then be easy to make players select their color in the gamelobby:
- Make a "change color" dropdown option in the gamelobby; preventing double use of colors.
- Make a "ready" checkmark for each player in the lobby - so that the game can only start when every player is ready. Readying up disables the color dropdown.

- Broadcast to all players fix
Every game instance should have its own n-threaded threadpool, where n is the amount of initial players.
When a broadcast happens, this should never be synchronous - always queue game communication commands to clients on the game threadpool.

- GameMapChangedCommand
at the start of the game, the map is sent via singular "tilechangedcommands".
I want you to make a ServerToClientCommand "GameMapChangedCommand" that will send the entire map in one command.
Change startup to use this command for faster map load at start.

---

# Implementation Plan

## Issue 1: Generic Player Type in Game Class

### Current State
- `Game` class uses `Map<Integer, Player>` for player storage
- `TurrestGameMode01.gameTick()` uses `instanceof Turrest01Player` check every tick
- `resyncPlayer()` also uses `instanceof` check
- `sendInitialMapToPlayers()` uses `instanceof` check

### Problem
The `instanceof` pattern is:
1. Runtime overhead on every tick
2. Not type-safe at compile time
3. Verbose and error-prone

### Solution

#### Backend Changes

**File:** `src/main/java/be/lefief/game/Game.java`

Make `Game` generic with bounded type parameter:

```java
@Getter
public abstract class Game<T extends Player> {
    private Map<Integer, T> playerByNumber;  // Now type-safe
    private UUID gameID;
    private UUID lobbyHostId;

    @Setter
    private boolean gameIsRunning = true;

    @Setter
    private Runnable onGameEnd;

    public Game(List<ClientSession> players, UUID lobbyHostId) {
        playerByNumber = new HashMap<>();
        gameID = UUID.randomUUID();
        this.lobbyHostId = lobbyHostId;
        for (int i = 0; i < players.size(); i++) {
            playerByNumber.put(i, createPlayer(players.get(i), i, gameID));
        }
    }

    /**
     * Factory method for creating players. Must be implemented by subclasses.
     */
    protected abstract T createPlayer(ClientSession session, int playerNumber, UUID gameId);

    public abstract void start();
    public abstract void stop();
    protected abstract void resyncPlayer(T player);  // Now takes T

    public void broadcastToAllPlayers(ServerToClientCommand command) {
        playerByNumber.values().forEach(player -> player.getClientSession().sendCommand(command));
    }

    public void reconnectPlayer(UUID userId, ClientSession newSession) {
        for (T player : playerByNumber.values()) {
            ClientSession oldSession = player.getClientSession();
            if (oldSession != null && userId.equals(oldSession.getClientID())) {
                player.setClientSession(newSession);
                player.setConnected(true);
                resyncPlayer(player);
                return;
            }
        }
    }

    // ... rest unchanged but use T instead of Player where appropriate
}
```

**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`

Update to use generic parameter:

```java
public class TurrestGameMode01 extends Game<Turrest01Player> {

    @Override
    protected Turrest01Player createPlayer(ClientSession session, int playerNumber, UUID gameId) {
        return new Turrest01Player(session, playerNumber, gameId);
    }

    // Remove getTurrest01Player() helper - no longer needed!

    @Override
    protected void resyncPlayer(Turrest01Player player) {
        // No instanceof check needed - player is already Turrest01Player
        if (getGameMap() == null || player.getClientSession() == null)
            return;

        // Send map tiles...

        // Direct access to resources - no instanceof!
        PlayerResources resources = player.getResources();
        player.getClientSession().sendCommand(new ResourceUpdateResponse(
                resources.getWood(),
                resources.getStone(),
                resources.getGold()
        ));
    }

    private void gameTick() {
        // ...
        for (Turrest01Player player : getPlayerByNumber().values()) {
            // No instanceof check needed!
            if (player.isConnected()) {
                PlayerResources resources = player.getResources();
                resources.addProduction();
                // ...
            }
        }
    }
}
```

**File:** `src/main/java/be/lefief/game/GameService.java`

Update to use wildcard or raw type where needed:

```java
private Map<UUID, Game<?>> activeGames = new ConcurrentHashMap<>();

// Or if specific game type known:
public void registerGame(Game<?> game) {
    activeGames.put(game.getGameID(), game);
}
```

---

## Issue 2: Player Color Selection

### Current State
- `PlayerColors` class has 16 colors
- Colors assigned by player number automatically
- No player choice in lobby

### Solution

#### Backend Changes

**File:** `src/main/java/be/lefief/game/Player.java`

Add color field:

```java
@Data
public class Player {
    private ClientSession clientSession;
    private Integer playerNumber;
    private UUID gameID;
    private boolean connected = true;
    private int colorIndex;  // NEW: Index into PlayerColors array

    public Player(ClientSession clientSession, Integer playerNumber, UUID gameID) {
        this.clientSession = clientSession;
        this.playerNumber = playerNumber;
        this.gameID = gameID;
        this.connected = true;
        this.colorIndex = playerNumber;  // Default: color matches player number
    }

    public String getColor() {
        return PlayerColors.getColor(colorIndex);
    }
}
```

**File:** `src/main/java/be/lefief/sockets/commands/client/send/ChangeColorCommand.java` (NEW)

```java
public class ChangeColorCommand {
    public static final String TOPIC = "CHANGE_COLOR";
}
```

**File:** `src/main/java/be/lefief/lobby/LobbyManager.java` or relevant handler

Add color change handling in lobby:

```java
public void handleColorChange(UUID playerId, int newColorIndex) {
    Lobby lobby = findLobbyByPlayer(playerId);
    if (lobby == null) return;

    // Check if color already taken
    if (lobby.isColorTaken(newColorIndex)) {
        sendError(playerId, "Color already taken by another player");
        return;
    }

    LobbyPlayer player = lobby.getPlayer(playerId);
    player.setColorIndex(newColorIndex);

    // Broadcast updated lobby state
    broadcastLobbyState(lobby);
}
```

**File:** `src/main/java/be/lefief/lobby/LobbyPlayer.java` (or similar)

Add ready state and color:

```java
public class LobbyPlayer {
    private UUID id;
    private String name;
    private int colorIndex;
    private boolean ready;
}
```

**File:** `src/main/java/be/lefief/lobby/Lobby.java`

Add ready check:

```java
public boolean allPlayersReady() {
    return players.values().stream().allMatch(LobbyPlayer::isReady);
}

public boolean isColorTaken(int colorIndex) {
    return players.values().stream()
            .anyMatch(p -> p.getColorIndex() == colorIndex);
}
```

#### Frontend Changes

**File:** `frontend/src/app/features/lobby/lobby-room/lobby-room.component.ts`

Add color selection and ready toggle:

```typescript
template: `
  <div class="room-container">
    <!-- ... existing header ... -->

    <div class="room-content">
      <div class="room-main">
        @if (lobbyService.activeLobby(); as lobby) {
          <div class="lobby-info-card">
            <h2>Players</h2>
            @for (player of lobbyPlayers(); track player.id) {
              <div class="player-row">
                <div class="player-color" [style.background-color]="getPlayerColor(player.colorIndex)"></div>
                <span class="player-name">{{ player.name }}</span>
                @if (player.id === myId) {
                  <select
                    [disabled]="player.ready"
                    [value]="player.colorIndex"
                    (change)="changeColor($event)">
                    @for (color of availableColors(); track color.index) {
                      <option [value]="color.index">{{ color.name }}</option>
                    }
                  </select>
                  <label class="ready-toggle">
                    <input type="checkbox" [checked]="player.ready" (change)="toggleReady()">
                    Ready
                  </label>
                } @else {
                  @if (player.ready) {
                    <span class="ready-badge">Ready</span>
                  }
                }
              </div>
            }
          </div>

          <div class="actions">
            @if (isHost()) {
              <button
                class="btn-primary btn-start"
                [disabled]="!allPlayersReady()"
                (click)="startGame()">
                Start Game
              </button>
              @if (!allPlayersReady()) {
                <p class="waiting-text">Waiting for all players to ready up...</p>
              }
            }
          </div>
        }
      </div>
    </div>
  </div>
`,

// Component class:
lobbyPlayers = signal<LobbyPlayer[]>([]);

availableColors(): { index: number, name: string }[] {
  const takenColors = this.lobbyPlayers()
    .filter(p => p.id !== this.myId)
    .map(p => p.colorIndex);

  return PLAYER_COLORS.map((color, index) => ({
    index,
    name: COLOR_NAMES[index],
    disabled: takenColors.includes(index)
  })).filter(c => !c.disabled || c.index === this.myColorIndex);
}

changeColor(event: Event): void {
  const colorIndex = +(event.target as HTMLSelectElement).value;
  this.socketService.sendCommand('LOBBY', 'CHANGE_COLOR', { colorIndex });
}

toggleReady(): void {
  this.socketService.sendCommand('LOBBY', 'TOGGLE_READY', {});
}

allPlayersReady(): boolean {
  return this.lobbyPlayers().every(p => p.ready);
}
```

**File:** `frontend/src/app/shared/constants/player-colors.ts`

Add color names:

```typescript
export const COLOR_NAMES: string[] = [
  'Red', 'Blue', 'Green', 'Orange', 'Purple', 'Cyan',
  'Amber', 'Pink', 'Deep Purple', 'Teal', 'Light Green',
  'Deep Orange', 'Indigo', 'Lime', 'Brown', 'Blue Grey'
];
```

---

## Issue 3: Async Broadcast with Game ThreadPool

### Current State
- `broadcastToAllPlayers()` sends commands synchronously in a forEach loop
- If one client is slow, it blocks all others
- No dedicated thread pool for game communication

### Solution

#### Backend Changes

**File:** `src/main/java/be/lefief/game/Game.java`

Add game-specific thread pool:

```java
@Getter
public abstract class Game<T extends Player> {
    private Map<Integer, T> playerByNumber;
    private UUID gameID;
    private UUID lobbyHostId;
    private ExecutorService communicationPool;  // NEW

    @Setter
    private boolean gameIsRunning = true;

    @Setter
    private Runnable onGameEnd;

    public Game(List<ClientSession> players, UUID lobbyHostId) {
        playerByNumber = new HashMap<>();
        gameID = UUID.randomUUID();
        this.lobbyHostId = lobbyHostId;

        // Create thread pool sized for player count
        this.communicationPool = Executors.newFixedThreadPool(
            players.size(),
            r -> {
                Thread t = new Thread(r, "Game-" + gameID.toString().substring(0, 8) + "-comm");
                t.setDaemon(true);
                return t;
            }
        );

        for (int i = 0; i < players.size(); i++) {
            playerByNumber.put(i, createPlayer(players.get(i), i, gameID));
        }
    }

    /**
     * Broadcasts command to all players asynchronously.
     * Each player's send runs on the game's thread pool.
     */
    public void broadcastToAllPlayers(ServerToClientCommand command) {
        for (T player : playerByNumber.values()) {
            if (player.isConnected() && player.getClientSession() != null) {
                communicationPool.submit(() -> {
                    try {
                        player.getClientSession().sendCommand(command);
                    } catch (Exception e) {
                        LOG.error("Failed to send command to player {}", player.getPlayerNumber(), e);
                    }
                });
            }
        }
    }

    /**
     * Sends command to a single player asynchronously.
     */
    public void sendToPlayer(T player, ServerToClientCommand command) {
        if (player.isConnected() && player.getClientSession() != null) {
            communicationPool.submit(() -> {
                try {
                    player.getClientSession().sendCommand(command);
                } catch (Exception e) {
                    LOG.error("Failed to send command to player {}", player.getPlayerNumber(), e);
                }
            });
        }
    }

    /**
     * Cleanup on game end.
     */
    protected void shutdownCommunicationPool() {
        if (communicationPool != null) {
            communicationPool.shutdown();
            try {
                if (!communicationPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    communicationPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                communicationPool.shutdownNow();
            }
        }
    }

    // Call in stop() method:
    public abstract void stop();
}
```

**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`

Update stop() to cleanup pool:

```java
@Override
public void stop() {
    running = false;
    setGameIsRunning(false);
    if (gameLoop != null) {
        gameLoop.shutdown();
        // ...
    }
    shutdownCommunicationPool();  // NEW: Cleanup communication pool
    LOG.info("Game stopped after {} ticks", tickCount);
}
```

---

## Issue 4: Bulk Map Command (GameMapChangedCommand)

### Current State
- Map sent tile-by-tile: `broadcastToAllPlayers(new TileUpdateResponse(x, y, tile))`
- For 100x100 map = 10,000 individual WebSocket messages
- Slow and inefficient for initial load

### Solution

#### Backend Changes

**File:** `src/main/java/be/lefief/game/turrest01/commands/FullMapResponse.java` (NEW)

```java
package be.lefief.game.turrest01.commands;

import be.lefief.game.map.GameMap;
import be.lefief.game.map.Structure;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.structure.TurrestBuilding;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the entire game map in a single command for efficient initial load.
 */
public class FullMapResponse extends ServerToClientCommand {

    public static final String TOPIC = "FULL_MAP";

    public FullMapResponse(GameMap gameMap) {
        super(ClientSocketSubject.GAME, TOPIC, createData(gameMap));
    }

    private static Map<String, Object> createData(GameMap gameMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("width", gameMap.getWidth());
        data.put("height", gameMap.getHeight());

        List<Map<String, Object>> tiles = new ArrayList<>();
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile tile = gameMap.getTile(x, y);
                if (tile != null) {
                    tiles.add(createTileData(x, y, tile));
                }
            }
        }
        data.put("tiles", tiles);

        return data;
    }

    private static Map<String, Object> createTileData(int x, int y, Tile tile) {
        Map<String, Object> tileData = new HashMap<>();
        tileData.put("x", x);
        tileData.put("y", y);
        tileData.put("terrainType", tile.getTerrainType().getTerrainTypeID());

        Structure structure = tile.getStructure();
        if (structure != null) {
            tileData.put("structureType", structure.getStructureTypeId());

            if (structure instanceof TurrestBuilding building) {
                tileData.put("buildingType", building.getBuildingTypeId());
                tileData.put("ownerPlayerNumber", building.getOwnerPlayerNumber());
            }
        }

        tileData.put("owners", new ArrayList<>(tile.getOwners()));

        return tileData;
    }
}
```

**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`

Update to use bulk map command:

```java
private void sendInitialMapToPlayers() {
    LOG.info("Sending initial map to all players");

    // First, send player info to each player
    for (Turrest01Player player : getPlayerByNumber().values()) {
        if (player.isConnected()) {
            sendToPlayer(player, new PlayerInfoResponse(player.getPlayerNumber()));
        }
    }

    // Send entire map in one command
    broadcastToAllPlayers(new FullMapResponse(gameMap));
    LOG.info("Sent full map ({}x{}) to all players", gameMap.getWidth(), gameMap.getHeight());

    // Send initial resources to each player
    for (Turrest01Player player : getPlayerByNumber().values()) {
        if (player.isConnected()) {
            PlayerResources resources = player.getResources();
            sendToPlayer(player, new ResourceUpdateResponse(
                    resources.getWood(),
                    resources.getStone(),
                    resources.getGold()
            ));
        }
    }
}
```

#### Frontend Changes

**File:** `frontend/src/app/features/game/game.component.ts`

Add handler for full map:

```typescript
ngOnInit(): void {
    // ... existing subscriptions ...

    // Listen for full map (bulk load)
    const fullMapSub = this.socketService.onCommand('GAME', 'FULL_MAP')
        .subscribe(cmd => {
            this.handleFullMap(cmd.data);
        });
    this.subscriptions.push(fullMapSub);
}

private handleFullMap(data: Record<string, any>): void {
    const width = data['width'] as number;
    const height = data['height'] as number;
    const tilesData = data['tiles'] as Array<Record<string, any>>;

    console.log(`Received full map: ${width}x${height} with ${tilesData.length} tiles`);

    this.mapMaxX = width;
    this.mapMaxY = height;
    this.tiles.clear();

    for (const tileData of tilesData) {
        const tile: Tile = {
            x: tileData['x'] as number,
            y: tileData['y'] as number,
            terrainType: tileData['terrainType'] as number,
            structureType: tileData['structureType'] as number | undefined,
            buildingType: tileData['buildingType'] as number | undefined,
            ownerPlayerNumber: tileData['ownerPlayerNumber'] as number | undefined,
            owners: tileData['owners'] as number[] || []
        };
        this.tiles.set(`${tile.x},${tile.y}`, tile);
    }

    this.hasReceivedTiles = true;
    this.render();
}
```

---

## Implementation Order

1. **Issue 1: Generic Player Type** (Backend refactoring)
   - Safest to do first, mostly compile-time changes
   - No frontend changes needed

2. **Issue 4: Bulk Map Command** (Backend + Frontend)
   - Performance improvement
   - Independent of other changes

3. **Issue 3: Async Broadcast** (Backend)
   - Improves performance and robustness
   - No frontend changes needed

4. **Issue 2: Player Color Selection** (Backend + Frontend)
   - Most complex, requires new lobby features
   - Depends on generic player type being done

---

## Files Summary

### Modified Files
- `src/main/java/be/lefief/game/Game.java` - Generic type, thread pool, async broadcast
- `src/main/java/be/lefief/game/Player.java` - Add colorIndex field
- `src/main/java/be/lefief/game/GameService.java` - Update for generic Game
- `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java` - Use generic, remove instanceof
- `src/main/java/be/lefief/lobby/Lobby.java` - Ready check, color validation
- `frontend/src/app/features/lobby/lobby-room/lobby-room.component.ts` - Color picker, ready toggle
- `frontend/src/app/features/game/game.component.ts` - Full map handler
- `frontend/src/app/shared/constants/player-colors.ts` - Add color names

### New Files
- `src/main/java/be/lefief/game/turrest01/commands/FullMapResponse.java`
- `src/main/java/be/lefief/sockets/commands/client/send/ChangeColorCommand.java`
- `src/main/java/be/lefief/sockets/commands/client/send/ToggleReadyCommand.java`

---

## Testing Checklist

- [ ] Game compiles with generic Game<T extends Player>
- [ ] No instanceof checks in TurrestGameMode01 game loop
- [ ] Thread pool created on game start with correct size
- [ ] Thread pool shutdown on game stop
- [ ] Broadcasts don't block on slow clients
- [ ] Full map received in single WebSocket message
- [ ] Frontend renders full map correctly
- [ ] Color dropdown shows available colors only
- [ ] Ready toggle disables color selection
- [ ] Start button only enabled when all players ready
- [ ] Color persists from lobby to game
