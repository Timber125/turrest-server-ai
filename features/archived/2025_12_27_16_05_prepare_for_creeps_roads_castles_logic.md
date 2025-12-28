Instructions:

These are changes meant for turrest-mode1 game.

1. The gameloop should stop changing tile types randomly - this was an example.
2. We want to introduce resources: "WOOD", "STONE", "GOLD". Players start with 100 each, and a base production of 1 per gametick.
3. We want to introduce buildings.
3.1 a tile can contain a structure. structure can be road or building for now. building is described in (4).
3.2 When player has no tile selected, the actions-panel (bottom-center) should contain buttons for each available building.
3.3 initial buildings:
3.3.1 Lumbercamp: 1x1 size - cost: 50 wood, 10 gold, 10 stone - adds 1 wood production per base tick
3.3.2 Stone quarry: 1x1 size - cost: 10 wood, 10 gold, 50 stone - adds 1 stone production per base tick
3.3.3 Gold mine: 1x1 size - cost: 10 wood, 50 gold, 10 stone - adds 1 gold production per base tick
3.4 When user clicks a building, user can hover the building over the map to see where it can be placed - showing red when not placeable, blue when placeable
3.5 When user builds a building, command is sent to backend - resources are subtracted - building is placed - BuildingChanged command is sent to all clients so they can update their view - and resources are subtracted from the player. If not enough resources, do not place the building and send out an error message "not enough resources" to the player who attempted to build.
4. We want to introduce roads.
4.1 at map creation time, we want to generate (random-walk) roads connecting each spawner to the castle. Maps will alwyas have 1 castle and N spawners so all spawners should random walk to the castle.
4.2 The roads are equal for all players - random-walk happens only once before cloning the map for each player.
4.3 A tile containing a road, cannot support any buildings
4.4 The road should be displayed neatly on the map
5. Unbuildable terrain types
5.1 each building should have an explicit list of which terrain types it can be built on.
5.2 Lumbercamp can only be built on FOREST tiles.
5.3 stone quarry can only be built on ROCKY tiles.
5.4 gold mine can only be built on DIRT tiles.

---

# Implementation Plan

## Architecture Principle

**General (reusable across game modes):**
- `Player.java` - base class, unchanged
- `Tile.java` - add structure support (tiles can contain a Structure)
- `Structure.java` - abstract base class for all structures
- `ErrorMessageResponse.java` - general error command

**Turrest01-specific (under `game/turrest01/` package):**
- All resources, buildings, roads, and game-specific commands

---

## Phase 1: Backend - Resource System (Turrest01-specific)

### 1.1 Create Resource Model
**File:** `src/main/java/be/lefief/game/turrest01/resource/ResourceType.java`
```java
public enum ResourceType {
    WOOD, STONE, GOLD
}
```

**File:** `src/main/java/be/lefief/game/turrest01/resource/PlayerResources.java`
- Fields: `Map<ResourceType, Integer> resources`, `Map<ResourceType, Integer> productionRates`
- Methods: `addProduction()`, `canAfford(ResourceCost)`, `subtract(ResourceCost)`, `getAmount(ResourceType)`
- Initialize with 100 of each resource, 1 production per tick

**File:** `src/main/java/be/lefief/game/turrest01/resource/ResourceCost.java`
- Fields: `int wood, int stone, int gold`
- Method: `boolean canAfford(PlayerResources)`

### 1.2 Create Turrest01Player
**File:** `src/main/java/be/lefief/game/turrest01/Turrest01Player.java`
```java
public class Turrest01Player extends Player {
    private PlayerResources resources;

    public Turrest01Player(ClientSession session, int playerNumber, UUID gameId) {
        super(session, playerNumber, gameId);
        this.resources = new PlayerResources();
    }
    // getters/setters
}
```

### 1.3 Create Resource Commands (Turrest01-specific)
**File:** `src/main/java/be/lefief/game/turrest01/commands/ResourceUpdateResponse.java`
- Topic: `RESOURCE_UPDATE`
- Data: `{ wood: int, stone: int, gold: int }`

**File:** `src/main/java/be/lefief/sockets/commands/client/reception/ErrorMessageResponse.java` (General)
- Topic: `ERROR_MESSAGE`
- Data: `{ message: string }`

### 1.4 Update TurrestGameMode01
**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`
- Change `Map<Integer, Player>` to `Map<Integer, Turrest01Player>`
- Override player creation to use `Turrest01Player`
- Remove random tile changes from `gameTick()`
- Add resource production: call `player.getResources().addProduction()` for each player
- Broadcast `ResourceUpdateResponse` to each player after production

---

## Phase 2: Backend - Structure System

### 2.1 Create General Structure Base (Global)
**File:** `src/main/java/be/lefief/game/map/Structure.java`
```java
public abstract class Structure {
    public abstract int getStructureTypeId();
}
```

### 2.2 Update Tile Model (Global)
**File:** `src/main/java/be/lefief/game/map/Tile.java`
- Add field: `Structure structure` (nullable)
- Add method: `boolean hasStructure()`
- Add method: `void setStructure(Structure)`
- Add method: `Structure getStructure()`

### 2.3 Create Turrest01 Structure Types
**File:** `src/main/java/be/lefief/game/turrest01/structure/TurrestStructureType.java`
```java
public enum TurrestStructureType {
    ROAD(0), BUILDING(1);
    // with getId()
}
```

**File:** `src/main/java/be/lefief/game/turrest01/structure/Road.java`
```java
public class Road extends Structure {
    @Override
    public int getStructureTypeId() { return TurrestStructureType.ROAD.getId(); }
}
```

**File:** `src/main/java/be/lefief/game/turrest01/structure/TurrestBuilding.java`
```java
public class TurrestBuilding extends Structure {
    private BuildingDefinition definition;
    private int ownerPlayerNumber;

    @Override
    public int getStructureTypeId() { return TurrestStructureType.BUILDING.getId(); }
}
```

### 2.4 Create Building Definitions (Turrest01-specific)
**File:** `src/main/java/be/lefief/game/turrest01/building/BuildingDefinition.java`
```java
public enum BuildingDefinition {
    LUMBERCAMP(1, "Lumbercamp",
        new ResourceCost(50, 10, 10),  // wood, stone, gold
        List.of(TerrainType.FOREST),
        Map.of(ResourceType.WOOD, 1)),
    STONE_QUARRY(2, "Stone Quarry",
        new ResourceCost(10, 50, 10),
        List.of(TerrainType.ROCKY),
        Map.of(ResourceType.STONE, 1)),
    GOLD_MINE(3, "Gold Mine",
        new ResourceCost(10, 10, 50),
        List.of(TerrainType.DIRT),
        Map.of(ResourceType.GOLD, 1));

    // Fields: id, name, cost, allowedTerrains, productionBonus
    public boolean canBuildOn(TerrainType terrain) { ... }
    public static BuildingDefinition fromId(int id) { ... }
}
```

---

## Phase 3: Backend - Road Generation (Turrest01-specific)

### 3.1 Create Road Generator
**File:** `src/main/java/be/lefief/game/turrest01/map/RoadGenerator.java`
- Input: `LevelLoader level` (contains tile grid before cloning)
- Algorithm:
  1. Find castle position (TerrainType.CASTLE)
  2. Find all spawner positions (TerrainType.SPAWNER)
  3. For each spawner, random-walk to castle:
     - Current = spawner position
     - While current != castle:
       - Calculate direction bias toward castle
       - Add randomness (70% toward castle, 30% random orthogonal)
       - Move to next tile, mark as road (unless CASTLE/SPAWNER/WATER)
       - Avoid revisiting tiles (use Set<Point>)
  4. Return `Set<Point>` of all road tiles

### 3.2 Integrate Road Generation
**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`
- In map creation:
  1. Load level via LevelLoader
  2. Call `RoadGenerator.generateRoads(level)` to get road positions
  3. Apply roads to level tiles (set structure = new Road())
  4. Create GameMap with road-modified level, clone for players

### 3.3 Update Tile Response (Turrest01-specific)
**File:** `src/main/java/be/lefief/game/turrest01/commands/TileUpdateResponse.java`
- Extends/replaces TileChangedResponse for turrest01
- Data: `{ x, y, terrainType, structureType?, buildingType?, ownerPlayerNumber? }`

---

## Phase 4: Backend - Building Placement (Turrest01-specific)

### 4.1 Create Build Command
**File:** `src/main/java/be/lefief/game/turrest01/commands/PlaceBuildingCommand.java`
- Data: `{ x: int, y: int, buildingType: int }`

### 4.2 Create Building Changed Response
**File:** `src/main/java/be/lefief/game/turrest01/commands/BuildingChangedResponse.java`
- Topic: `BUILDING_CHANGED`
- Data: `{ x: int, y: int, buildingType: int, playerNumber: int }`

### 4.3 Create Turrest01 Game Handler
**File:** `src/main/java/be/lefief/game/turrest01/handlers/Turrest01GameHandler.java`
- Method: `handlePlaceBuilding(command, session, game)`
- Logic:
  1. Get Turrest01Player from session
  2. Validate buildingType exists via `BuildingDefinition.fromId()`
  3. Get tile at (x, y) within player's section
  4. Validate: tile terrain in buildingDef.allowedTerrains
  5. Validate: !tile.hasStructure()
  6. Validate: player.getResources().canAfford(buildingDef.cost)
  7. If invalid: send ErrorMessageResponse
  8. If valid:
     - player.getResources().subtract(cost)
     - tile.setStructure(new TurrestBuilding(buildingDef, playerNumber))
     - player.getResources().addProductionBonus(buildingDef)
     - Broadcast BuildingChangedResponse to all players
     - Send ResourceUpdateResponse to building player

### 4.4 Register Handler in TurrestGameMode01
**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`
- Add method: `handleGameCommand(command, session)`
- Route PLACE_BUILDING to Turrest01GameHandler

---

## Phase 5: Frontend - Resource Display

### 5.1 Create Resource Model
**File:** `frontend/src/app/shared/models/game.model.ts`
- Add interface: `PlayerResources { wood: number, stone: number, gold: number }`
- Add to existing models

### 5.2 Create Resource Bar Component
**File:** `frontend/src/app/features/game/components/resource-bar/resource-bar.component.ts`
- Inputs: `wood`, `stone`, `gold`
- Display: Icons + amounts for each resource (top of screen)
- Styling: Resource icons (🪵 WOOD | 🪨 STONE | 🪙 GOLD)

### 5.3 Integrate into Game Component
**File:** `frontend/src/app/features/game/game.component.ts`
- Add state: `resources: PlayerResources = { wood: 100, stone: 100, gold: 100 }`
- Subscribe to `GAME:RESOURCE_UPDATE` command
- Pass resources to resource-bar component

---

## Phase 6: Frontend - Building Placement UI

### 6.1 Create Building Definitions
**File:** `frontend/src/app/shared/models/building.model.ts`
```typescript
interface BuildingDefinition {
    id: number;
    name: string;
    icon: string;
    cost: { wood: number, stone: number, gold: number };
    allowedTerrains: TerrainType[];
}

const BUILDINGS: BuildingDefinition[] = [
    { id: 1, name: 'Lumbercamp', icon: '🪓', cost: {wood:50,stone:10,gold:10}, allowedTerrains: [TerrainType.FOREST] },
    { id: 2, name: 'Stone Quarry', icon: '⛏️', cost: {wood:10,stone:50,gold:10}, allowedTerrains: [TerrainType.ROCKY] },
    { id: 3, name: 'Gold Mine', icon: '⚒️', cost: {wood:10,stone:10,gold:50}, allowedTerrains: [TerrainType.DIRT] }
];
```

### 6.2 Update Action Panel Component
**File:** `frontend/src/app/features/game/components/action-panel/action-panel.component.ts`
- Input: `selectedTile`, `resources`
- When no tile selected: show building buttons
- Each button shows: icon, name, cost
- Disable if can't afford
- Output: `buildingSelected` event with BuildingDefinition

### 6.3 Update Tile Model
**File:** `frontend/src/app/shared/models/game.model.ts`
- Update Tile interface: add `hasRoad?: boolean`, `building?: { type: number, name: string }`

### 6.4 Building Placement Mode in Game Component
**File:** `frontend/src/app/features/game/game.component.ts`
- Add state: `placementMode: BuildingDefinition | null`
- Add state: `hoverTile: {x, y} | null`
- On building selected: enter placement mode
- On mouse move over canvas: update hoverTile
- Render placement preview:
  - Blue overlay if `canPlace(hoverTile, placementMode)`
  - Red overlay if cannot place
- On canvas click in placement mode:
  - If valid: send PLACE_BUILDING command, exit placement mode
  - If invalid: show error, stay in placement mode
- ESC key: exit placement mode

### 6.5 Placement Validation (Frontend)
**File:** `frontend/src/app/features/game/game.component.ts`
- Method: `canPlace(tile, building): boolean`
  - Check: tile exists
  - Check: tile.terrainType in building.allowedTerrains
  - Check: !tile.hasRoad
  - Check: !tile.building
  - Check: resources >= building.cost

---

## Phase 7: Frontend - Road & Building Rendering

### 7.1 Update Tile Rendering
**File:** `frontend/src/app/features/game/game.component.ts`
- In `renderTile()`:
  - After terrain: if tile.hasRoad, draw road overlay (brown/tan path)
  - After road: if tile.building, draw building icon/sprite

### 7.2 Road Rendering Style
- Color: tan/brown (#c4a574)
- Style: draw as connected path segments based on neighboring roads
- Consider: simplified as solid color overlay initially, enhance to connected paths later

### 7.3 Building Rendering
- Draw building icon centered on tile
- Consider: simple colored square with building type indicator initially

---

## Phase 8: Error Handling & Polish

### 8.1 Error Message Display
**File:** `frontend/src/app/features/game/game.component.ts`
- Subscribe to `GAME:ERROR_MESSAGE`
- Show toast/notification with error message
- Auto-dismiss after 3 seconds

### 8.2 Initial Resource Sync
**File:** `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java`
- In `resyncPlayer()`: send current resources
- In initial map send: include road data in tiles

---

## Implementation Order

1. **Phase 1** - Resource system (backend) - Turrest01Player, resources
2. **Phase 5** - Resource display (frontend) - can test resource updates
3. **Phase 2** - Structure system (backend) - base Structure, turrest01 structures
4. **Phase 3** - Road generation (backend) - visible on map load
5. **Phase 7** - Road rendering (frontend)
6. **Phase 4** - Building placement (backend)
7. **Phase 6** - Building placement UI (frontend)
8. **Phase 8** - Error handling & polish

---

## Files Summary

### New Backend Files - General (reusable):
- `src/main/java/be/lefief/game/map/Structure.java` - abstract base class
- `src/main/java/be/lefief/sockets/commands/client/reception/ErrorMessageResponse.java`

### New Backend Files - Turrest01-specific:
**Resource System:**
- `src/main/java/be/lefief/game/turrest01/resource/ResourceType.java`
- `src/main/java/be/lefief/game/turrest01/resource/PlayerResources.java`
- `src/main/java/be/lefief/game/turrest01/resource/ResourceCost.java`
- `src/main/java/be/lefief/game/turrest01/Turrest01Player.java`

**Structure System:**
- `src/main/java/be/lefief/game/turrest01/structure/TurrestStructureType.java`
- `src/main/java/be/lefief/game/turrest01/structure/Road.java`
- `src/main/java/be/lefief/game/turrest01/structure/TurrestBuilding.java`

**Building System:**
- `src/main/java/be/lefief/game/turrest01/building/BuildingDefinition.java`

**Map:**
- `src/main/java/be/lefief/game/turrest01/map/RoadGenerator.java`

**Commands:**
- `src/main/java/be/lefief/game/turrest01/commands/ResourceUpdateResponse.java`
- `src/main/java/be/lefief/game/turrest01/commands/TileUpdateResponse.java`
- `src/main/java/be/lefief/game/turrest01/commands/BuildingChangedResponse.java`
- `src/main/java/be/lefief/game/turrest01/commands/PlaceBuildingCommand.java`

**Handlers:**
- `src/main/java/be/lefief/game/turrest01/handlers/Turrest01GameHandler.java`

### Modified Backend Files:
- `src/main/java/be/lefief/game/map/Tile.java` - add Structure support (general)
- `src/main/java/be/lefief/game/turrest01/TurrestGameMode01.java` - use Turrest01Player, resource ticks, road generation, remove random changes

### New Frontend Files:
- `frontend/src/app/features/game/components/resource-bar/resource-bar.component.ts`
- `frontend/src/app/shared/models/building.model.ts`

### Modified Frontend Files:
- `frontend/src/app/shared/models/game.model.ts` - add resource/structure interfaces
- `frontend/src/app/features/game/game.component.ts` - resources, placement mode, rendering
- `frontend/src/app/features/game/components/action-panel/action-panel.component.ts` - building buttons
- `frontend/src/app/features/game/components/tile-info/tile-info.component.ts` - show structure info

---

## Package Structure After Implementation

```
be.lefief.game/
├── Game.java (unchanged)
├── Player.java (unchanged)
├── GameService.java (unchanged)
├── map/
│   ├── Tile.java (modified - add Structure support)
│   ├── GameMap.java (unchanged)
│   ├── TerrainType.java (unchanged)
│   ├── Structure.java (NEW - abstract base)
│   └── LevelLoader.java (unchanged)
└── turrest01/
    ├── TurrestGameMode01.java (modified)
    ├── Turrest01Player.java (NEW)
    ├── resource/
    │   ├── ResourceType.java (NEW)
    │   ├── PlayerResources.java (NEW)
    │   └── ResourceCost.java (NEW)
    ├── structure/
    │   ├── TurrestStructureType.java (NEW)
    │   ├── Road.java (NEW)
    │   └── TurrestBuilding.java (NEW)
    ├── building/
    │   └── BuildingDefinition.java (NEW)
    ├── map/
    │   └── RoadGenerator.java (NEW)
    ├── commands/
    │   ├── ResourceUpdateResponse.java (NEW)
    │   ├── TileUpdateResponse.java (NEW)
    │   ├── BuildingChangedResponse.java (NEW)
    │   └── PlaceBuildingCommand.java (NEW)
    └── handlers/
        └── Turrest01GameHandler.java (NEW)
```
