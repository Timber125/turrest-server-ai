Instructions:

These are changes meant for turrest-mode1 game.

1. The map is initially cloned once for each player. I want you to make it so that every player is only able to build on their own piece of land. Keep in mind that later on, we will add features to convert land ownership from player to player so that it is possible to "capture building land" of another player - this will be implemented later.
2. The roads are not connecting castle to spawner. I want this logic to be fixed so that roads will always connect spawner to castle.
3. The roads are looking really weird. I want you to create overlay PNGs for roads so that we can overlay roads on their terrain type. Roads should connect visually to neighbouring tiles that also contain roads.

---

# Implementation Plan

## Issue 1: Tile-Based Land Ownership System

### Current State
- Map is cloned horizontally for each player (player 0 at x=0, player 1 at x=sectionWidth, etc.)
- No explicit ownership data on tiles
- `Turrest01GameHandler.handlePlaceBuilding()` validates tile but doesn't check ownership

### Design Goals
- Each tile stores a **list of players** who can build on it (supports multi-owner scenarios)
- Ownership is set during map generation (not calculated from position)
- Support up to **16 players** with visually distinct colors
- Tiles display a **colored contour** indicating ownership
- Current gamemode uses single owner per tile, but system supports future multi-owner mechanics

### Solution

#### Player Colors (16 distinct colors)

```java
// Backend: be.lefief.game.PlayerColors.java
public class PlayerColors {
    public static final String[] COLORS = {
        "#E53935",  // 0: Red
        "#1E88E5",  // 1: Blue
        "#43A047",  // 2: Green
        "#FB8C00",  // 3: Orange
        "#8E24AA",  // 4: Purple
        "#00ACC1",  // 5: Cyan
        "#FFB300",  // 6: Amber
        "#D81B60",  // 7: Pink
        "#5E35B1",  // 8: Deep Purple
        "#00897B",  // 9: Teal
        "#7CB342",  // 10: Light Green
        "#F4511E",  // 11: Deep Orange
        "#3949AB",  // 12: Indigo
        "#C0CA33",  // 13: Lime
        "#6D4C41",  // 14: Brown
        "#546E7A"   // 15: Blue Grey
    };

    public static String getColor(int playerNumber) {
        return COLORS[playerNumber % COLORS.length];
    }
}
```

```typescript
// Frontend: shared/constants/player-colors.ts
export const PLAYER_COLORS: string[] = [
    '#E53935',  // 0: Red
    '#1E88E5',  // 1: Blue
    '#43A047',  // 2: Green
    '#FB8C00',  // 3: Orange
    '#8E24AA',  // 4: Purple
    '#00ACC1',  // 5: Cyan
    '#FFB300',  // 6: Amber
    '#D81B60',  // 7: Pink
    '#5E35B1',  // 8: Deep Purple
    '#00897B',  // 9: Teal
    '#7CB342',  // 10: Light Green
    '#F4511E',  // 11: Deep Orange
    '#3949AB',  // 12: Indigo
    '#C0CA33',  // 13: Lime
    '#6D4C41',  // 14: Brown
    '#546E7A'   // 15: Blue Grey
];

export function getPlayerColor(playerNumber: number): string {
    return PLAYER_COLORS[playerNumber % PLAYER_COLORS.length];
}
```

#### Backend Changes

**File:** `src/main/java/be/lefief/game/map/Tile.java`
- Add ownership field to Tile:

```java
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

public class Tile {
    private TerrainType terrainType;
    private Structure structure;
    private Set<Integer> owners = new HashSet<>();  // Players who can build here

    // Ownership methods
    public Set<Integer> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    public void addOwner(int playerNumber) {
        owners.add(playerNumber);
    }

    public void removeOwner(int playerNumber) {
        owners.remove(playerNumber);
    }

    public void setOwners(Set<Integer> newOwners) {
        owners.clear();
        owners.addAll(newOwners);
    }

    public boolean canPlayerBuild(int playerNumber) {
        return owners.contains(playerNumber);
    }

    public boolean hasOwners() {
        return !owners.isEmpty();
    }
}
```

**File:** `src/main/java/be/lefief/game/map/GameMap.java`
- Update `copyLevelToMap()` to assign ownership during map generation:

```java
private void copyLevelToMap(LevelLoader level, int offsetX, int offsetY,
                            Set<Point> roadPositions, Supplier<Structure> roadSupplier,
                            int ownerPlayerNumber) {  // Add owner parameter
    for (int x = 0; x < level.getWidth(); x++) {
        for (int y = 0; y < level.getHeight(); y++) {
            TerrainType terrain = level.getTerrainAt(x, y);
            Tile tile = new Tile(terrain);

            // Assign ownership to this player's section
            tile.addOwner(ownerPlayerNumber);

            // Apply road if this position has one
            if (roadSupplier != null && roadPositions.contains(new Point(x, y))) {
                tile.setStructure(roadSupplier.get());
            }

            map[offsetX + x][offsetY + y] = tile;
        }
    }
}
```

- Update constructor to pass player number:

```java
// In GameMap constructor:
for (int playerNum = 0; playerNum < playerCount; playerNum++) {
    int offsetX = playerNum * playerSectionWidth;
    copyLevelToMap(level, offsetX, 0, roadPositions, roadSupplier, playerNum);
}
```

**File:** `src/main/java/be/lefief/game/turrest01/commands/TileUpdateResponse.java`
- Include ownership data in tile updates:

```java
private static Map<String, Object> createData(int x, int y, Tile tile) {
    Map<String, Object> data = new HashMap<>();
    data.put("x", x);
    data.put("y", y);
    data.put("terrainType", tile.getTerrainType().getId());

    // Structure data...

    // Add ownership data
    data.put("owners", new ArrayList<>(tile.getOwners()));

    return data;
}
```

**File:** `src/main/java/be/lefief/game/turrest01/handlers/Turrest01GameHandler.java`
- Add ownership validation using tile data:

```java
// After getting tile, before terrain validation:
if (!tile.canPlayerBuild(player.getPlayerNumber())) {
    LOG.info("Player {} cannot build on tile ({}, {}) - not their territory",
             player.getPlayerNumber(), x, y);
    clientSession.sendCommand(new ErrorMessageResponse("Cannot build on another player's territory"));
    return;
}
```

**New File:** `src/main/java/be/lefief/game/PlayerColors.java`
- Contains the 16 player colors (see above)

#### Frontend Changes

**New File:** `frontend/src/app/shared/constants/player-colors.ts`
- Contains the 16 player colors (see above)

**File:** `frontend/src/app/shared/models/game.model.ts`
- Update Tile interface:

```typescript
export interface Tile {
    x: number;
    y: number;
    terrainType: TerrainType;
    structureType?: StructureType;
    buildingType?: number;
    ownerPlayerNumber?: number;  // For buildings
    owners?: number[];           // Players who can build here
}
```

**File:** `frontend/src/app/features/game/game.component.ts`

1. Track current player number (from server):
```typescript
myPlayerNumber: number = 0;  // Set from server on game start
```

2. Update `handleTileUpdate()` to include owners:
```typescript
const tile: Tile = {
    x, y, terrainType,
    structureType,
    buildingType,
    ownerPlayerNumber,
    owners: data['owners'] as number[] || []
};
```

3. Update `canPlaceBuilding()`:
```typescript
private canPlaceBuilding(tile: Tile, building: BuildingDefinition): boolean {
    // Check ownership
    if (!tile.owners || !tile.owners.includes(this.myPlayerNumber)) {
        return false;
    }
    // ... rest of validation
}
```

4. Add ownership contour rendering in `drawTile()`:
```typescript
private drawTile(tile: Tile): void {
    // ... draw terrain, road, building ...

    // Draw ownership contour
    if (tile.owners && tile.owners.length > 0) {
        this.drawOwnershipContour(screenX, screenY, tile.owners);
    }
}

private drawOwnershipContour(screenX: number, screenY: number, owners: number[]): void {
    const contourWidth = 2;

    if (owners.length === 1) {
        // Single owner: full contour in owner's color
        this.ctx.strokeStyle = getPlayerColor(owners[0]);
        this.ctx.lineWidth = contourWidth;
        this.ctx.strokeRect(
            screenX + contourWidth / 2,
            screenY + contourWidth / 2,
            this.tileSize - contourWidth,
            this.tileSize - contourWidth
        );
    } else {
        // Multiple owners: split contour (each side can be different color)
        // For simplicity: divide border into segments for each owner
        const segmentCount = owners.length;
        const sidesPerOwner = 4 / segmentCount;

        owners.forEach((owner, index) => {
            this.ctx.strokeStyle = getPlayerColor(owner);
            this.ctx.lineWidth = contourWidth;
            this.ctx.beginPath();

            // Draw portion of border based on owner index
            const startSide = index * sidesPerOwner;
            this.drawPartialBorder(screenX, screenY, startSide, sidesPerOwner);

            this.ctx.stroke();
        });
    }
}

private drawPartialBorder(x: number, y: number, startSide: number, sideCount: number): void {
    const w = this.tileSize;
    const h = this.tileSize;
    const offset = 1;

    // Sides: 0=top, 1=right, 2=bottom, 3=left
    for (let i = 0; i < sideCount && startSide + i < 4; i++) {
        const side = Math.floor(startSide + i) % 4;
        switch (side) {
            case 0: // Top
                this.ctx.moveTo(x + offset, y + offset);
                this.ctx.lineTo(x + w - offset, y + offset);
                break;
            case 1: // Right
                this.ctx.moveTo(x + w - offset, y + offset);
                this.ctx.lineTo(x + w - offset, y + h - offset);
                break;
            case 2: // Bottom
                this.ctx.moveTo(x + w - offset, y + h - offset);
                this.ctx.lineTo(x + offset, y + h - offset);
                break;
            case 3: // Left
                this.ctx.moveTo(x + offset, y + h - offset);
                this.ctx.lineTo(x + offset, y + offset);
                break;
        }
    }
}
```

5. Add visual feedback for non-owned tiles in placement mode:
```typescript
private drawPlacementPreview(): void {
    // ... existing code ...

    // Additional check: is this my territory?
    const isMyTerritory = tile.owners?.includes(this.myPlayerNumber) ?? false;
    if (!isMyTerritory) {
        // Draw "forbidden" indicator
        this.ctx.fillStyle = 'rgba(100, 100, 100, 0.5)';
        this.ctx.fillRect(screenX, screenY, this.tileSize, this.tileSize);
    }
}
```

---

## Issue 2: Fix Road Generation (Spawner → Castle)

### Current State
- `RoadGenerator.randomWalkToTarget()` walks from spawner to castle
- Algorithm may get stuck or not reach the castle
- Uses random walk with 70% bias toward castle

### Problem Analysis
The random walk algorithm can get stuck if:
1. All valid moves lead to already-visited tiles
2. Water or other obstacles block the path
3. The algorithm terminates when "adjacent to" castle but may not create a connected path

### Solution

**File:** `src/main/java/be/lefief/game/turrest01/map/RoadGenerator.java`

Replace random walk with A* pathfinding or improved algorithm:

```java
/**
 * Generates a guaranteed path from start to target using BFS/A*.
 * Falls back to random walk if direct path fails.
 */
private Set<Point> generatePath(LevelLoader level, Point start, Point target) {
    // Use BFS for guaranteed shortest path
    Queue<Point> queue = new LinkedList<>();
    Map<Point, Point> cameFrom = new HashMap<>();
    Set<Point> visited = new HashSet<>();

    queue.add(start);
    visited.add(start);
    cameFrom.put(start, null);

    while (!queue.isEmpty()) {
        Point current = queue.poll();

        if (isAdjacentTo(current, target)) {
            // Reconstruct path
            return reconstructPath(cameFrom, current);
        }

        for (Point neighbor : getValidNeighbors(level, current, visited)) {
            visited.add(neighbor);
            cameFrom.put(neighbor, current);
            queue.add(neighbor);
        }
    }

    LOG.warn("BFS failed to find path from ({},{}) to ({},{}), using fallback",
             start.x, start.y, target.x, target.y);
    return randomWalkToTarget(level, start, target); // Fallback
}

private Set<Point> reconstructPath(Map<Point, Point> cameFrom, Point end) {
    Set<Point> path = new LinkedHashSet<>();
    Point current = end;
    while (current != null && cameFrom.get(current) != null) {
        if (canPlaceRoad(level.getTerrainAt(current.x, current.y))) {
            path.add(current);
        }
        current = cameFrom.get(current);
    }
    return path;
}
```

Also update `generateRoads()` to use the new method.

---

## Issue 3: Road Visual Improvements with Connected Overlays

### Current State
- Roads rendered as simple tan squares in `game.component.ts`
- No visual connection between adjacent road tiles
- No PNG assets exist

### Solution

#### Create Road Overlay Assets

Create 16 road tile variations based on which neighbors have roads (similar to autotiling):

```
Naming convention: road_NESW.png (N=north, E=east, S=south, W=west)
- road_0000.png - isolated road (no connections)
- road_1000.png - connects north only
- road_0100.png - connects east only
- road_0010.png - connects south only
- road_0001.png - connects west only
- road_1100.png - connects north+east (corner)
- road_1010.png - connects north+south (vertical)
- road_0101.png - connects east+west (horizontal)
- road_1001.png - connects north+west (corner)
- road_0110.png - connects east+south (corner)
- road_0011.png - connects south+west (corner)
- road_1110.png - T-junction (N+E+S)
- road_1101.png - T-junction (N+E+W)
- road_1011.png - T-junction (N+S+W)
- road_0111.png - T-junction (E+S+W)
- road_1111.png - crossroads (all 4)
```

**Location:** `frontend/src/assets/tiles/roads/`

#### Backend Changes

**File:** `src/main/java/be/lefief/game/turrest01/commands/TileUpdateResponse.java`
- Ensure road connection data is sent (or let frontend compute it)

#### Frontend Changes

**File:** `frontend/src/app/features/game/game.component.ts`

1. Load road tile images on init:
```typescript
private roadImages: Map<string, HTMLImageElement> = new Map();

private loadRoadImages(): void {
    const variations = ['0000','1000','0100','0010','0001','1100','1010','0101',
                        '1001','0110','0011','1110','1101','1011','0111','1111'];
    for (const v of variations) {
        const img = new Image();
        img.src = `assets/tiles/roads/road_${v}.png`;
        this.roadImages.set(v, img);
    }
}
```

2. Calculate road connections when rendering:
```typescript
private getRoadVariation(x: number, y: number): string {
    const n = this.hasRoad(x, y - 1) ? '1' : '0';
    const e = this.hasRoad(x + 1, y) ? '1' : '0';
    const s = this.hasRoad(x, y + 1) ? '1' : '0';
    const w = this.hasRoad(x - 1, y) ? '1' : '0';
    return n + e + s + w;
}

private hasRoad(x: number, y: number): boolean {
    const tile = this.tiles.get(`${x},${y}`);
    return tile?.structureType === StructureType.ROAD;
}
```

3. Update `drawTile()` to use road images:
```typescript
if (tile.structureType === StructureType.ROAD) {
    const variation = this.getRoadVariation(tile.x, tile.y);
    const img = this.roadImages.get(variation);
    if (img && img.complete) {
        this.ctx.drawImage(img, screenX, screenY, this.tileSize, this.tileSize);
    } else {
        // Fallback to colored square
        this.ctx.fillStyle = '#c4a574';
        this.ctx.fillRect(screenX, screenY, this.tileSize, this.tileSize);
    }
}
```

---

## Implementation Order

1. **Issue 2: Fix Road Generation** (Backend only, highest priority)
   - Roads must connect properly before we improve visuals

2. **Issue 1: Restrict Building to Player Territory** (Backend + minor Frontend)
   - Add ownership check in handler
   - Add helper methods to GameMap
   - Optional: grey out non-owned tiles on frontend

3. **Issue 3: Road Visual Improvements** (Frontend + Assets)
   - Create road PNG assets (16 variations)
   - Implement autotiling logic in frontend
   - Load and render road images

---

## Files Summary

### Modified Files
- `src/main/java/be/lefief/game/map/GameMap.java` - add ownership methods
- `src/main/java/be/lefief/game/turrest01/handlers/Turrest01GameHandler.java` - add ownership validation
- `src/main/java/be/lefief/game/turrest01/map/RoadGenerator.java` - fix pathfinding algorithm
- `frontend/src/app/features/game/game.component.ts` - road rendering, ownership visuals

### New Files
- `frontend/src/assets/tiles/roads/road_XXXX.png` (16 files) - road tile variations

---

## Testing Checklist


- [ ] Roads connect spawner to castle reliably
- [ ] Player 0 can only build in their section (x: 0 to sectionWidth-1)
- [ ] Player 1 can only build in their section (x: sectionWidth to 2*sectionWidth-1)
- [ ] Error message shown when building on another player's land
- [ ] Road tiles visually connect to neighbors
- [ ] All 16 road variations render correctly
- [ ] Fallback rendering works when images fail to load
