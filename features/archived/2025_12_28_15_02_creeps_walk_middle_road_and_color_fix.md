Instructions:

- Creeps are moving at the side of the tiles, they should walk the middle of the tile/road.
- Creeps have a colored contour - this is nice: keep the possibility to color the outlinings. But for creeps spawned by _waves_ the outlining should be transparent / invisible
  (we're going to add "send creeps" functionality later, and when a player sends creeps we want this contour to be colored in their color. More on this later, but keep the functionality)
- would love a nice ghost & troll sprite to go with the creeps... can you fix?

---

## Implementation Plan

### Issue 1: Creeps Walking on Side of Tiles (Double Offset Bug)

**Root Cause:**
- Backend (`Creep.java:26-27`): Creep positions already include `+0.5` offset to center on tiles
- Frontend (`game.component.ts:942-954`): Adds another `+tileSize/2` offset when calculating `centerX`/`centerY`
- Result: Creeps are offset by 1.5 tiles instead of 0.5

**Fix in `frontend/src/app/features/game/game.component.ts`:**
```typescript
// Current (WRONG):
const screenX = creep.x * this.tileSize - this.cameraX;
const centerX = screenX + this.tileSize / 2;  // <-- Double offset!

// Fixed:
const centerX = creep.x * this.tileSize - this.cameraX;  // x already includes +0.5
const centerY = creep.y * this.tileSize - this.cameraY;  // y already includes +0.5
```

Update all references to use `centerX`/`centerY` directly instead of `screenX`/`screenY`.

---

### Issue 2: Wave-Spawned Creeps Should Have No Contour Color

**Design:** Use `spawnedByPlayer` field instead of a boolean flag:
- `null` → creep was spawned by a wave (no contour)
- `Integer` (player number) → creep was sent by that player (colored contour)

**Changes Required:**

1. **Backend - Add `spawnedByPlayer` field to Creep:**
   - `Creep.java`: Add `private final Integer spawnedByPlayer` field (nullable)
   - Constructor: Accept `Integer spawnedByPlayer` parameter
   - `CreepManager.java`: Pass `null` when spawning from waves

2. **Backend - Send field in SpawnCreepCommand:**
   - `SpawnCreepCommand.java`: Add `data.put("spawnedByPlayer", creep.getSpawnedByPlayer())` (will be null for waves)

3. **Frontend - Update Creep model:**
   - `game.model.ts`: Add `spawnedByPlayer: number | null` to `Creep` interface

4. **Frontend - Conditional contour rendering:**
   ```typescript
   // In drawCreeps():
   if (creep.spawnedByPlayer !== null) {
     // Use the sending player's color for the contour
     const senderColorIndex = this.playerColorMap.get(creep.spawnedByPlayer) ?? creep.spawnedByPlayer;
     this.ctx.strokeStyle = getPlayerColor(senderColorIndex);
     this.ctx.lineWidth = 2;
     this.ctx.stroke();
   }
   // else: no stroke for wave-spawned creeps (spawnedByPlayer is null)
   ```

---

### Issue 3: Ghost & Troll Sprites

**Implementation:**

1. **Create sprite directory:**
   - `frontend/src/assets/sprites/creeps/`

2. **Create sprite images (32x32 PNG with transparency):**
   - `ghost.png` - Translucent ghostly figure
   - `troll.png` - Bulky troll figure

3. **Load sprites in game component:**
   ```typescript
   private creepSprites: Map<string, HTMLImageElement> = new Map();

   private loadCreepSprites(): void {
     const types = ['ghost', 'troll'];
     for (const type of types) {
       const img = new Image();
       img.src = `/assets/sprites/creeps/${type}.png`;
       this.creepSprites.set(type.toUpperCase(), img);
     }
   }
   ```

4. **Update drawCreeps() to use sprites:**
   ```typescript
   const sprite = this.creepSprites.get(creep.creepType);
   if (sprite && sprite.complete) {
     // Draw sprite centered on position
     this.ctx.drawImage(sprite, centerX - size/2, centerY - size/2, size, size);

     // Draw contour ring around sprite (only if sent by a player)
     if (creep.spawnedByPlayer !== null) {
       const senderColorIndex = this.playerColorMap.get(creep.spawnedByPlayer) ?? creep.spawnedByPlayer;
       this.ctx.beginPath();
       this.ctx.arc(centerX, centerY, size/2 + 2, 0, Math.PI * 2);
       this.ctx.strokeStyle = getPlayerColor(senderColorIndex);
       this.ctx.lineWidth = 2;
       this.ctx.stroke();
     }
   } else {
     // Fallback to colored circle (current behavior)
   }
   ```

---

## File Changes Summary

| File | Changes |
|------|---------|
| `frontend/.../game.component.ts` | Fix position offset, load sprites, update drawCreeps |
| `frontend/.../game.model.ts` | Add `spawnedByPlayer: number | null` to Creep interface |
| `src/.../creep/Creep.java` | Add `spawnedByPlayer` field (Integer, nullable) |
| `src/.../commands/SpawnCreepCommand.java` | Include `spawnedByPlayer` in data |
| `src/.../creep/CreepManager.java` | Pass `null` for wave spawns |
| `frontend/src/assets/sprites/creeps/` | Add ghost.png, troll.png |

---

## Testing Checklist

- [ ] Creeps walk in the center of roads/tiles
- [ ] Wave-spawned creeps have no colored outline
- [ ] Sprites display correctly for ghost and troll types
- [ ] Health bars still display correctly above sprites
- [ ] Performance is acceptable with many creeps on screen 