Instructions:

- Creeps are still not using sprites, i see gray circles for ghosts and brown circles for trolls now
- The "selected tile" section in frontend should add extra tile information: which structure is built there if any
- When game is over, losing player gets game over popup, but winning player screen freezes until losing player goes back to lobby. these popups should appear at the same time.
- Resource updates in frontend seem to hang occasionally - is this due to memory issues on my machine or can we improve on the robustness of "increase resources every game tick in frontend"


---

## Implementation Plan

### Issue 1: Creeps not using sprites (fallback circles appearing)

**Analysis:**
The sprite loading code in `game.component.ts` (lines 683-692) lacks an `onload` handler. The images are loaded asynchronously, but the first render may occur before images are loaded. Additionally, there's no tracking of load status.

**Solution:**
1. Add `onload` tracking for sprite images
2. Force a re-render when sprites finish loading
3. Add error logging to confirm paths are correct

**Files to modify:**
- `frontend/src/app/features/game/game.component.ts`

**Changes:**
```typescript
private loadCreepSprites(): void {
  const creepTypes = ['GHOST', 'TROLL'];
  for (const type of creepTypes) {
    const img = new Image();
    img.onload = () => {
      console.log(`Loaded creep sprite: ${type}`);
      // Already in animation loop, will pick up on next frame
    };
    img.onerror = () => {
      console.error(`Failed to load creep sprite: /assets/sprites/creeps/${type.toLowerCase()}.png`);
    };
    img.src = `/assets/sprites/creeps/${type.toLowerCase()}.png`;
    this.creepSprites.set(type, img);
  }
}
```

Also verify the Angular assets configuration includes the sprites folder.

---

### Issue 2: Tile info should show structure information

**Analysis:**
The `tile-info.component.ts` currently only displays position and terrain type. Users want to see:
- Structure type (Road, Building, or None)
- Building type and name if applicable
- Building owner if applicable

**Solution:**
1. Pass additional data about structures and buildings to the component
2. Update the template to display structure/building info
3. Add helper methods for getting structure/building names

**Files to modify:**
- `frontend/src/app/features/game/components/tile-info/tile-info.component.ts`

**Changes:**
- Import `StructureType` and `BUILDING_DEFINITIONS`
- Add new template sections for Structure and Building info:
```html
@if (selectedTile?.structureType !== undefined) {
  <p><span class="label">Structure:</span> {{ getStructureName(selectedTile.structureType) }}</p>
}
@if (selectedTile?.buildingType !== undefined) {
  <p><span class="label">Building:</span> {{ getBuildingName(selectedTile.buildingType) }}</p>
}
@if (selectedTile?.ownerPlayerNumber !== undefined) {
  <p><span class="label">Owner:</span> Player {{ selectedTile.ownerPlayerNumber }}</p>
}
```

---

### Issue 3: Game over popup not synchronized between players

**Analysis:**
Looking at `TurrestGameMode01.java:handlePlayerDeath()` (lines 202-224):
1. First broadcasts loser message: `GameOverCommand(loser.playerNumber, false)`
2. Then broadcasts winner message: `GameOverCommand(winner.playerNumber, true)`

The frontend `handleGameOver()` logic (lines 905-920):
- For `isWinner=true`: ALL players see the popup (winner sees "Victory", others see "Player X wins")
- For `isWinner=false`: ONLY the loser sees the popup

**The bug:** When the loser's message arrives, the winner ignores it (doesn't match any condition). The winner should see the popup when `isWinner=true` message arrives, but perhaps there's a race condition or the message isn't being received.

**Solution:**
Change the game over logic to be more robust:
1. Track game over state even for non-winner/non-loser scenarios
2. Ensure the winner popup appears immediately when the winner message broadcasts

**Files to modify:**
- `frontend/src/app/features/game/game.component.ts`
- Optionally verify backend sends both messages correctly

**Changes to frontend:**
```typescript
private handleGameOver(data: Record<string, any>): void {
  const playerNumber = data['playerNumber'] as number;
  const isWinner = data['isWinner'] as boolean;

  // Always process game over when a winner is announced
  if (isWinner) {
    this.gameOver = true;
    if (playerNumber === this.myPlayerNumber) {
      this.gameOverMessage = 'Victory! You are the last player standing!';
    } else {
      this.gameOverMessage = `Player ${playerNumber} wins!`;
    }
  } else if (playerNumber === this.myPlayerNumber) {
    // I lost
    this.gameOver = true;
    this.gameOverMessage = 'Game Over - Your castle has fallen!';
  }
  // Note: If someone else loses but game continues (>2 players), don't show popup
}
```

Also add console logging to debug if messages are being received.

---

### Issue 4: Resource updates occasionally hanging

**Analysis:**
Resource updates are sent every game tick (1 second) via WebSocket. The frontend uses `ngZone.run()` which should trigger change detection. Potential causes:
1. WebSocket message backlog
2. No throttling/debouncing on updates
3. Memory pressure from continuous object creation
4. Animation frame blocking UI updates

**Solution:**
1. Add defensive null checks
2. Use `ChangeDetectorRef.detectChanges()` explicitly after resource updates
3. Consider using `OnPush` change detection for the ResourceBarComponent
4. Add console logging to verify updates are being received

**Files to modify:**
- `frontend/src/app/features/game/game.component.ts`
- `frontend/src/app/features/game/components/resource-bar/resource-bar.component.ts` (optional optimization)

**Changes:**
```typescript
// In game.component.ts, modify handleResourceUpdate:
private handleResourceUpdate(data: Record<string, any>): void {
  const wood = data['wood'] as number;
  const stone = data['stone'] as number;
  const gold = data['gold'] as number;

  // Only update if values actually changed (avoid unnecessary change detection)
  if (this.resources.wood !== wood ||
      this.resources.stone !== stone ||
      this.resources.gold !== gold) {
    this.resources = { wood, stone, gold };
  }
}
```

Optionally inject `ChangeDetectorRef` and call `markForCheck()` or `detectChanges()` for more reliable updates.

---

## Implementation Order

1. **Issue 1** (Sprites) - Quick fix, add onload/onerror handlers and verify asset paths
2. **Issue 2** (Tile info) - Add structure display to tile-info component
3. **Issue 3** (Game over) - Add logging first to verify message delivery, then fix logic
4. **Issue 4** (Resources) - Add optimization to reduce unnecessary updates

