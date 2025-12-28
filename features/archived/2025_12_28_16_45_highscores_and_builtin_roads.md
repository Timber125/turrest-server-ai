Instructions: 

- Terrain type "SPAWNER" and "CASTLE" should always contain a road, by default. 
-> This should make it so that the creep spawnpoint (center of spawner) will be occupied by road; and that the despawn point (center of castle) will also be occupied by a road. 
Double check the actual despawn point, feels like creeps are despawned 1 tile early on the road instead of in the castle. 

- Currently, every player sees his own color and HP at the top right of the screen;
-> I'd like to add a rank-list at the bottom left of the screen, that will show each player's name in their color, and their score; this should act as a ranklist, ordered by highest-score top, lowest-score bottom
-> Score is equal to remaining HP for the Turrest01 game mode, but should be abstract for other games. add the score component to the general "Player" class.

---

## Implementation Plan

### Part 1: Roads on SPAWNER and CASTLE Tiles

**Problem Analysis:**
- Current `RoadGenerator.java` explicitly excludes SPAWNER and CASTLE tiles from road placement via `canPlaceRoad()` method
- Creeps spawn at spawner center (`spawnerPosition.x + 0.5, spawnerPosition.y + 0.5`) and follow the path
- Creeps despawn when `currentPathIndex >= path.size()` - meaning they despawn at the LAST road tile, not at the castle center
- This causes the "1 tile early" despawn issue because the path ends at the last road tile before the castle

**Solution:**

#### 1.1 Modify RoadGenerator to include SPAWNER and CASTLE in paths
**File:** `src/main/java/be/lefief/game/turrest01/map/RoadGenerator.java`

```java
// Change canPlaceRoad to allow SPAWNER and CASTLE
private boolean canPlaceRoad(TerrainType terrain) {
    return terrain != null &&
           terrain != TerrainType.WATER_SHALLOW &&
           terrain != TerrainType.WATER_DEEP;
    // Remove: terrain != TerrainType.CASTLE && terrain != TerrainType.SPAWNER
}
```

#### 1.2 Verify Creep Path Includes Castle
**File:** `src/main/java/be/lefief/game/turrest01/creep/PathFinder.java`

Ensure `findPath()` includes the castle tile as the final destination, so creeps visually reach the castle center before despawning.

#### 1.3 Update Road Structure to Handle Special Terrains
**File:** `src/main/java/be/lefief/game/turrest01/structure/Road.java`

Roads on SPAWNER/CASTLE should render differently or be invisible (since these tiles already have their own visual representation).

**Tasks:**
- [ ] Modify `RoadGenerator.canPlaceRoad()` to allow SPAWNER/CASTLE
- [ ] Ensure path reconstruction includes start (spawner) and end (castle) points
- [ ] Update `PathFinder.findPath()` to include castle as final waypoint
- [ ] Test that creeps now despawn at castle center, not 1 tile before

---

### Part 2: Abstract Score in Player Class

**Current State:**
- `Player.java` (base class): Has `playerNumber`, `gameID`, `connected`, `colorIndex`
- `Turrest01Player.java`: Extends Player, has `hitpoints`, `resources`

**Solution:**

#### 2.1 Add Abstract Score to Base Player Class
**File:** `src/main/java/be/lefief/game/Player.java`

```java
@Data
public abstract class Player {
    private ClientSession clientSession;
    private Integer playerNumber;
    private UUID gameID;
    private boolean connected = true;
    private int colorIndex;

    // Abstract score method - each game mode defines what "score" means
    public abstract int getScore();

    // For display purposes
    public abstract String getScoreLabel(); // e.g., "HP", "Points", "Gold"
}
```

#### 2.2 Implement Score in Turrest01Player
**File:** `src/main/java/be/lefief/game/turrest01/Turrest01Player.java`

```java
@Override
public int getScore() {
    return hitpoints; // Score = remaining HP
}

@Override
public String getScoreLabel() {
    return "HP";
}
```

#### 2.3 Create ScoreboardCommand for Broadcasting Player Rankings
**File:** `src/main/java/be/lefief/game/turrest01/commands/ScoreboardCommand.java` (NEW)

```java
public class ScoreboardCommand extends ServerToClientCommand {
    public ScoreboardCommand(List<PlayerScoreEntry> entries) {
        super(ServerSocketSubject.GAME, "SCOREBOARD", new HashMap<>() {{
            put("players", entries.stream().map(e -> Map.of(
                "playerNumber", e.getPlayerNumber(),
                "colorIndex", e.getColorIndex(),
                "username", e.getUsername(),
                "score", e.getScore(),
                "isAlive", e.isAlive()
            )).collect(Collectors.toList()));
        }});
    }
}
```

#### 2.4 Broadcast Scoreboard on HP Changes
**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`

In `checkCastleReached()` and `handlePlayerDeath()`, broadcast updated scoreboard:

```java
private void broadcastScoreboard() {
    List<PlayerScoreEntry> entries = getPlayerByNumber().values().stream()
        .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore())) // Descending
        .map(p -> new PlayerScoreEntry(
            p.getPlayerNumber(),
            p.getColorIndex(),
            p.getClientSession().getUserName(),
            p.getScore(),
            p.isAlive()
        ))
        .collect(Collectors.toList());

    broadcastToAllPlayers(new ScoreboardCommand(entries));
}
```

**Tasks:**
- [ ] Add abstract `getScore()` and `getScoreLabel()` to `Player.java`
- [ ] Implement score methods in `Turrest01Player.java`
- [ ] Create `PlayerScoreEntry` data class
- [ ] Create `ScoreboardCommand.java`
- [ ] Broadcast scoreboard after game start and on each HP change

---

### Part 3: Frontend Ranklist Component

#### 3.1 Create Ranklist Component
**File:** `frontend/src/app/features/game/components/ranklist/ranklist.component.ts` (NEW)

```typescript
@Component({
  selector: 'app-ranklist',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ranklist">
      <h4>Rankings</h4>
      <div class="rank-entries">
        @for (entry of players; track entry.playerNumber; let i = $index) {
          <div class="rank-entry" [class.eliminated]="!entry.isAlive">
            <span class="rank">#{{ i + 1 }}</span>
            <span class="color-dot" [style.background]="getPlayerColor(entry.colorIndex)"></span>
            <span class="name">{{ entry.username }}</span>
            <span class="score">{{ entry.score }}</span>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .ranklist {
      background: #1a1a2e;
      border: 1px solid #333;
      border-radius: 8px;
      padding: 0.5rem;
    }
    h4 { margin: 0 0 0.5rem; color: #fff; font-size: 0.9rem; }
    .rank-entries { display: flex; flex-direction: column; gap: 4px; }
    .rank-entry {
      display: flex; align-items: center; gap: 8px;
      padding: 4px 8px; background: #0f0f23; border-radius: 4px;
    }
    .rank-entry.eliminated { opacity: 0.5; text-decoration: line-through; }
    .rank { color: #888; font-size: 0.8rem; width: 24px; }
    .color-dot { width: 12px; height: 12px; border-radius: 50%; border: 1px solid #fff; }
    .name { color: #fff; flex: 1; font-size: 0.85rem; }
    .score { color: #00d9ff; font-weight: bold; font-size: 0.9rem; }
  `]
})
export class RanklistComponent {
  @Input() players: PlayerScoreEntry[] = [];

  getPlayerColor(colorIndex: number): string {
    return PLAYER_COLORS[colorIndex] || '#888';
  }
}
```

#### 3.2 Add Model for Player Score Entry
**File:** `frontend/src/app/shared/models/game.model.ts`

```typescript
export interface PlayerScoreEntry {
  playerNumber: number;
  colorIndex: number;
  username: string;
  score: number;
  isAlive: boolean;
}
```

#### 3.3 Integrate into GameComponent
**File:** `frontend/src/app/features/game/game.component.ts`

- Add `RanklistComponent` to imports
- Add `scoreboard: PlayerScoreEntry[] = []` property
- Subscribe to `SCOREBOARD` socket command
- Position ranklist at bottom-left (absolute positioning or grid)

```typescript
// In template, add to bottom-left:
<app-ranklist [players]="scoreboard" class="ranklist-overlay"></app-ranklist>

// CSS:
.ranklist-overlay {
  position: absolute;
  bottom: 200px; // Above bottom pane
  left: 10px;
  z-index: 100;
  min-width: 180px;
}
```

#### 3.4 Subscribe to Scoreboard Updates
```typescript
// In ngOnInit:
const scoreboardSub = this.socketService.onCommand('GAME', 'SCOREBOARD')
  .subscribe(cmd => {
    this.scoreboard = cmd.data['players'] as PlayerScoreEntry[];
  });
```

**Tasks:**
- [ ] Create `PlayerScoreEntry` interface in `game.model.ts`
- [ ] Create `RanklistComponent` standalone component
- [ ] Add `SCOREBOARD` to `SocketTopic` enum
- [ ] Subscribe to scoreboard updates in `GameComponent`
- [ ] Position ranklist at bottom-left of game area
- [ ] Style to match existing dark theme

---

### Implementation Order

1. **Backend - Part 1 (Roads):** Fix road generation to include spawner/castle
2. **Backend - Part 2 (Score):** Add abstract score and broadcast scoreboard
3. **Frontend - Part 3 (Ranklist):** Create and integrate ranklist component
4. **Testing:** Verify creeps despawn at castle and ranklist updates correctly

### Files to Modify

| File | Changes |
|------|---------|
| `RoadGenerator.java` | Allow roads on SPAWNER/CASTLE terrain |
| `PathFinder.java` | Ensure path ends at castle center |
| `Player.java` | Add abstract `getScore()`, `getScoreLabel()` |
| `Turrest01Player.java` | Implement score = HP |
| `TurrestGameMode01.java` | Broadcast scoreboard on changes |
| `ScoreboardCommand.java` | NEW - Command to send rankings |
| `PlayerScoreEntry.java` | NEW - Data class for ranking entry |
| `game.model.ts` | Add `PlayerScoreEntry` interface |
| `socket-command.model.ts` | Add `SCOREBOARD` topic |
| `ranklist.component.ts` | NEW - Ranklist UI component |
| `game.component.ts` | Integrate ranklist, subscribe to updates |

