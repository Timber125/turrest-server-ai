Instruction: 

We tried to create a bottom bar in frontend: 

[20% | 60% | 20%] 

where the left panel shows "selected tile information" (terrain type & later more); the middle panel should show all available actions on the selected tile (create a dummy "build turret" button for now); the right panel should show an age of empires II style minimap indicating what part of the map we are currently viewing. 

Most of this should already be available, but not sure where we left off. 

--------------

# Frontend Tile Selection and Bottom Bar Plan

The goal is to implement a robust, modular bottom bar for the game UI, splitting the existing monolithic `GameComponent` into dedicated sub-components for Tile Info, Actions, and Minimap. This will improve maintainability and allow for easier future enhancements (like "Age of Empires II style" specific details).

## User Review Required

> [!NOTE]
> The current implementation in `GameComponent` already contains most of the required logic. This plan focuses on **refactoring** and **polishing** rather than writing from scratch.
> I will assume "Age of Empires II style" minimap primarily means functionality (navigation, viewport rectangle) which is currently present but will be cleaned up.

## Proposed Changes

### Frontend - Game Feature

#### [NEW] `src/app/features/game/components/tile-info/tile-info.component.ts` (and html/scss)
- **Responsibility**: Display selected tile details (position, terrain, preview).
- **Inputs**: `selectedTile: Tile | null`.

#### [NEW] `src/app/features/game/components/action-panel/action-panel.component.ts` (and html/scss)
- **Responsibility**: Display available actions (Build Turret, etc.).
- **Inputs**: `selectedTile: Tile | null`.
- **Outputs**: `actionTriggered` events (e.g., `createTurret`).

#### [NEW] `src/app/features/game/components/minimap/minimap.component.ts` (and html/scss)
- **Responsibility**: Render minimap and handle navigation.
- **Inputs**: `tiles: Map<string, Tile>`, `cameraX`, `cameraY`, `zoom` (or viewport rect).
- **Outputs**: `cameraMove` event (new x, y).
- **Logic**: Move the minimap rendering and event handling logic from `GameComponent`.

#### [MODIFY] `src/app/features/game/game.component.ts` (and template/style)
- **Changes**: 
    - Remove inline HTML for bottom pane.
    - Remove inline CSS for bottom pane.
    - Remove minimap rendering/interaction logic (delegate to `MinimapComponent`).
    - Use `<app-tile-info>`, `<app-action-panel>`, `<app-minimap>` in the template.

## Verification Plan

### Automated Tests
- Run `ng test` to ensure no regressions in basic component instantiation.
- (Optional) Add unit tests for the new components if time permits.

### Manual Verification
1.  **Launch Game**: Start the app and enter a game.
2.  **Minimap**:
    -   Verify the map renders correctly (colors match terrain).
    -   Verify the white viewport rectangle matches the main camera view.
    -   Click on the minimap -> Camera should jump to location.
    -   Drag on the minimap -> Camera should follow smoothly.
3.  **Tile Selection**:
    -   Click a tile on main map -> Left panel should update with correct terrain/coords.
    -   Click empty space/drag -> Selection should behave consistently (keep or clear).
4.  **Actions**:
    -   Select a valid build tile (Grass/Dirt) -> "Create Turret" button enables.
    -   Select invalid tile -> Button disables.
    -   Click button -> Verify console (or mock) output triggers.
