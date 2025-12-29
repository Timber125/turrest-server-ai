Instruction: 

- I want a standard menu popup when pressing escape like in most games - in multiplayer, game continues playing. Later in singleplayer this will pause the game. 
# Continue 
# Options 
   - Keymapping & Sound config
# Stats 
   - Shows game stats / graphs and numbers 
# Quit 


This means that: 
- We will map all actions on what seem to be logical default keys so we can perform actions with our keyboard 
- We will add options to configure sound (music / effects / master)
- Server will keep track of stats for a game (and remove them from memory when all players have quit) which players can request (via opening menu > stats)

And i want an export / import option for keymappings

---

## Implementation Plan (Revised)

### Design Principles Applied

1. **Type Safety**: Use TypeScript enums instead of magic strings for actions
2. **Single Responsibility**: Separate settings persistence from audio playback
3. **Input Context**: Handle conflicting keybindings with context-aware priority
4. **Schema Versioning**: Support settings migration across versions
5. **Event-Driven Stats**: Use generic event system for extensible stat collection
6. **Modifier Key Support**: Allow Ctrl+Key, Shift+Key combinations

---

### Current Architecture

**Keyboard Handling:**
- `game.component.ts` has `@HostListener('window:keydown')` - currently only handles Escape for placement mode
- No keymapping system exists

**Audio System:**
- `AudioService` exists with `setVolume()`, `mute()`, `toggleMute()` methods
- No persistence - settings lost on page refresh
- No music system (only sound effects)

**Settings Persistence:**
- Only `auth.service.ts` uses sessionStorage for user data
- No settings service or localStorage usage

**Modal Patterns:**
- Game uses overlay pattern with `position: absolute`, `z-index`, and `@if` conditionals
- `game-over-overlay` style provides the modal template

---

### Part 1: Input System Architecture

#### 1.1 Define Input Actions (Type-Safe Enums)

**New File: `frontend/src/app/shared/models/input-action.model.ts`**

```typescript
/**
 * All bindable game actions. Using enum provides:
 * - Type safety (no typos in action strings)
 * - IDE autocomplete
 * - Refactoring support
 * - Single source of truth
 */
export enum InputAction {
  // Menu
  MENU_TOGGLE = 'menu.toggle',
  MENU_STATS = 'menu.stats',

  // Selection/Placement
  SELECTION_CANCEL = 'selection.cancel',
  PLACEMENT_CONFIRM = 'placement.confirm',

  // Camera
  CAMERA_RESET = 'camera.reset',
  ZOOM_IN = 'zoom.in',
  ZOOM_OUT = 'zoom.out',

  // Buildings (dynamically extended based on BuildingDefinition)
  BUILD_LUMBERCAMP = 'building.lumbercamp',
  BUILD_STONE_QUARRY = 'building.stoneQuarry',
  BUILD_GOLD_MINE = 'building.goldMine',

  // Towers
  TOWER_BASIC = 'tower.basic',

  // Creeps (1-9 reserved for creep hotkeys)
  SEND_CREEP_1 = 'creep.slot1',
  SEND_CREEP_2 = 'creep.slot2',
  SEND_CREEP_3 = 'creep.slot3',
}

/**
 * Input context determines which actions are active.
 * Higher priority contexts consume keys first.
 */
export enum InputContext {
  TEXT_INPUT = 0,    // Typing in input fields - blocks all game keys
  MODAL = 1,         // Menu/modal open - limited keys work
  PLACEMENT = 2,     // Placing building/tower - cancel key active
  GAME = 3,          // Normal gameplay - all keys active
}

/**
 * Metadata for each action including display name and default binding.
 */
export interface InputActionMeta {
  action: InputAction;
  displayName: string;
  category: 'menu' | 'camera' | 'building' | 'tower' | 'creep';
  defaultBinding: KeyBinding;
  contexts: InputContext[];  // Which contexts this action works in
}

export interface KeyBinding {
  key: string;           // e.g., 'Q', 'Escape', 'F1'
  ctrl?: boolean;
  shift?: boolean;
  alt?: boolean;
}

export const INPUT_ACTION_REGISTRY: InputActionMeta[] = [
  {
    action: InputAction.MENU_TOGGLE,
    displayName: 'Toggle Menu',
    category: 'menu',
    defaultBinding: { key: 'Escape' },
    contexts: [InputContext.GAME, InputContext.MODAL, InputContext.PLACEMENT],
  },
  {
    action: InputAction.SELECTION_CANCEL,
    displayName: 'Cancel Selection',
    category: 'menu',
    defaultBinding: { key: 'Escape' },
    contexts: [InputContext.PLACEMENT],  // Only in placement mode
  },
  {
    action: InputAction.CAMERA_RESET,
    displayName: 'Reset Camera',
    category: 'camera',
    defaultBinding: { key: 'Home' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.ZOOM_IN,
    displayName: 'Zoom In',
    category: 'camera',
    defaultBinding: { key: '+' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.ZOOM_OUT,
    displayName: 'Zoom Out',
    category: 'camera',
    defaultBinding: { key: '-' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.BUILD_LUMBERCAMP,
    displayName: 'Build Lumbercamp',
    category: 'building',
    defaultBinding: { key: 'Q' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.BUILD_STONE_QUARRY,
    displayName: 'Build Stone Quarry',
    category: 'building',
    defaultBinding: { key: 'W' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.BUILD_GOLD_MINE,
    displayName: 'Build Gold Mine',
    category: 'building',
    defaultBinding: { key: 'E' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.TOWER_BASIC,
    displayName: 'Build Basic Tower',
    category: 'tower',
    defaultBinding: { key: 'T' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.SEND_CREEP_1,
    displayName: 'Send Creep (Slot 1)',
    category: 'creep',
    defaultBinding: { key: '1' },
    contexts: [InputContext.GAME],
  },
  {
    action: InputAction.SEND_CREEP_2,
    displayName: 'Send Creep (Slot 2)',
    category: 'creep',
    defaultBinding: { key: '2' },
    contexts: [InputContext.GAME],
  },
  // ... etc
];
```

#### 1.2 Input Service with Context Awareness

**New File: `frontend/src/app/core/services/input.service.ts`**

```typescript
@Injectable({ providedIn: 'root' })
export class InputService {
  private readonly settingsService = inject(SettingsService);

  // Current input context stack (highest priority first)
  private contextStack = signal<InputContext[]>([InputContext.GAME]);

  // Action stream for subscribers
  private actionSubject = new Subject<InputAction>();
  readonly action$ = this.actionSubject.asObservable();

  // Rebinding mode
  private rebindingAction = signal<InputAction | null>(null);
  private rebindCallback: ((binding: KeyBinding) => void) | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('keydown', (e) => this.handleKeyDown(e));
    }
  }

  /**
   * Push a context onto the stack (e.g., when opening menu).
   */
  pushContext(context: InputContext): void {
    this.contextStack.update(stack => [context, ...stack]);
  }

  /**
   * Remove a context from the stack (e.g., when closing menu).
   */
  popContext(context: InputContext): void {
    this.contextStack.update(stack => stack.filter(c => c !== context));
  }

  /**
   * Get current active context (highest priority).
   */
  get currentContext(): InputContext {
    return this.contextStack()[0] ?? InputContext.GAME;
  }

  /**
   * Start listening for next key to rebind an action.
   */
  startRebinding(action: InputAction): Promise<KeyBinding> {
    return new Promise((resolve) => {
      this.rebindingAction.set(action);
      this.rebindCallback = resolve;
    });
  }

  /**
   * Cancel rebinding mode.
   */
  cancelRebinding(): void {
    this.rebindingAction.set(null);
    this.rebindCallback = null;
  }

  private handleKeyDown(event: KeyboardEvent): void {
    // Skip if in text input
    if (this.isTextInput(event.target)) {
      return;
    }

    // Handle rebinding mode
    if (this.rebindingAction() !== null) {
      event.preventDefault();
      const binding: KeyBinding = {
        key: event.key,
        ctrl: event.ctrlKey,
        shift: event.shiftKey,
        alt: event.altKey,
      };
      this.rebindCallback?.(binding);
      this.cancelRebinding();
      return;
    }

    // Find matching action for current context
    const currentContext = this.currentContext;
    const bindings = this.settingsService.getKeyBindings();

    for (const meta of INPUT_ACTION_REGISTRY) {
      // Check if action is valid in current context
      if (!meta.contexts.includes(currentContext)) {
        continue;
      }

      const binding = bindings.get(meta.action) ?? meta.defaultBinding;
      if (this.matchesBinding(event, binding)) {
        event.preventDefault();
        this.actionSubject.next(meta.action);
        return;  // First match wins (context priority)
      }
    }
  }

  private matchesBinding(event: KeyboardEvent, binding: KeyBinding): boolean {
    const keyMatches = event.key.toUpperCase() === binding.key.toUpperCase();
    const ctrlMatches = !binding.ctrl || event.ctrlKey;
    const shiftMatches = !binding.shift || event.shiftKey;
    const altMatches = !binding.alt || event.altKey;

    return keyMatches && ctrlMatches && shiftMatches && altMatches;
  }

  private isTextInput(target: EventTarget | null): boolean {
    if (!target) return false;
    const element = target as HTMLElement;
    return (
      element instanceof HTMLInputElement ||
      element instanceof HTMLTextAreaElement ||
      element.isContentEditable
    );
  }
}
```

---

### Part 2: Settings Service with Schema Versioning

**New File: `frontend/src/app/core/services/settings.service.ts`**

```typescript
/**
 * Settings schema version - increment when structure changes.
 * Enables migration of old settings to new format.
 */
const SETTINGS_VERSION = 1;

interface SettingsSchema {
  version: number;
  audio: AudioSettings;
  keyBindings: Record<string, KeyBinding>;  // InputAction -> KeyBinding
}

interface AudioSettings {
  masterVolume: number;  // 0-1
  musicVolume: number;   // 0-1
  effectsVolume: number; // 0-1
  muted: boolean;
}

const DEFAULT_SETTINGS: SettingsSchema = {
  version: SETTINGS_VERSION,
  audio: {
    masterVolume: 0.5,
    musicVolume: 0.3,
    effectsVolume: 0.5,
    muted: false,
  },
  keyBindings: {},  // Empty = use defaults from registry
};

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly STORAGE_KEY = 'turrest_settings';

  // Reactive settings state
  private settings = signal<SettingsSchema>(DEFAULT_SETTINGS);

  // Derived audio settings for convenience
  readonly masterVolume = computed(() => this.settings().audio.masterVolume);
  readonly musicVolume = computed(() => this.settings().audio.musicVolume);
  readonly effectsVolume = computed(() => this.settings().audio.effectsVolume);
  readonly muted = computed(() => this.settings().audio.muted);

  constructor() {
    this.loadSettings();
  }

  /**
   * Get effective volume for effects (master * effects).
   */
  getEffectiveEffectsVolume(): number {
    if (this.muted()) return 0;
    return this.masterVolume() * this.effectsVolume();
  }

  /**
   * Get effective volume for music (master * music).
   */
  getEffectiveMusicVolume(): number {
    if (this.muted()) return 0;
    return this.masterVolume() * this.musicVolume();
  }

  /**
   * Get key bindings map.
   */
  getKeyBindings(): Map<InputAction, KeyBinding> {
    const map = new Map<InputAction, KeyBinding>();
    const bindings = this.settings().keyBindings;

    for (const [action, binding] of Object.entries(bindings)) {
      map.set(action as InputAction, binding);
    }

    return map;
  }

  /**
   * Update a single key binding.
   */
  setKeyBinding(action: InputAction, binding: KeyBinding): void {
    this.settings.update(s => ({
      ...s,
      keyBindings: { ...s.keyBindings, [action]: binding },
    }));
    this.saveSettings();
  }

  /**
   * Update audio settings.
   */
  setAudioSettings(audio: Partial<AudioSettings>): void {
    this.settings.update(s => ({
      ...s,
      audio: { ...s.audio, ...audio },
    }));
    this.saveSettings();
  }

  /**
   * Reset all key bindings to defaults.
   */
  resetKeyBindingsToDefaults(): void {
    this.settings.update(s => ({
      ...s,
      keyBindings: {},
    }));
    this.saveSettings();
  }

  /**
   * Export key bindings as JSON string.
   */
  exportKeyBindings(): string {
    return JSON.stringify(this.settings().keyBindings, null, 2);
  }

  /**
   * Import key bindings from JSON string.
   * Returns error message on failure, null on success.
   */
  importKeyBindings(json: string): string | null {
    try {
      const bindings = JSON.parse(json);

      // Validate structure
      if (typeof bindings !== 'object') {
        return 'Invalid format: expected object';
      }

      // Validate each binding
      for (const [action, binding] of Object.entries(bindings)) {
        if (!Object.values(InputAction).includes(action as InputAction)) {
          return `Unknown action: ${action}`;
        }
        if (!this.isValidBinding(binding)) {
          return `Invalid binding for ${action}`;
        }
      }

      this.settings.update(s => ({
        ...s,
        keyBindings: bindings,
      }));
      this.saveSettings();
      return null;
    } catch {
      return 'Invalid JSON format';
    }
  }

  private loadSettings(): void {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (!stored) return;

    try {
      const parsed = JSON.parse(stored);
      const migrated = this.migrateSettings(parsed);
      this.settings.set(migrated);
    } catch {
      console.warn('Failed to load settings, using defaults');
    }
  }

  private saveSettings(): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.settings()));
  }

  /**
   * Migrate settings from older versions.
   */
  private migrateSettings(settings: any): SettingsSchema {
    const version = settings.version ?? 0;

    if (version === SETTINGS_VERSION) {
      return settings;
    }

    // Migration from v0 (no version field)
    if (version === 0) {
      return {
        version: SETTINGS_VERSION,
        audio: {
          masterVolume: settings.masterVolume ?? 0.5,
          musicVolume: settings.musicVolume ?? 0.3,
          effectsVolume: settings.effectsVolume ?? 0.5,
          muted: false,
        },
        keyBindings: settings.keymap ?? {},
      };
    }

    // Future migrations go here
    // if (version === 1) { ... migrate to v2 ... }

    return DEFAULT_SETTINGS;
  }

  private isValidBinding(binding: any): binding is KeyBinding {
    return (
      typeof binding === 'object' &&
      typeof binding.key === 'string' &&
      (binding.ctrl === undefined || typeof binding.ctrl === 'boolean') &&
      (binding.shift === undefined || typeof binding.shift === 'boolean') &&
      (binding.alt === undefined || typeof binding.alt === 'boolean')
    );
  }
}
```

---

### Part 3: AudioService Integration

**Modify: `frontend/src/app/core/services/audio.service.ts`**

```typescript
@Injectable({ providedIn: 'root' })
export class AudioService {
  private readonly settingsService = inject(SettingsService);

  private audioContext: AudioContext | null = null;
  private sounds: Map<string, AudioBuffer> = new Map();
  private loaded = false;

  // Sound mappings for different event types
  private readonly SOUND_MAP: Record<string, string> = {
    kill: 'coin',
    hit: 'coin',
    build: 'build',
    tower: 'tower',
    send: 'send'
  };

  // ... existing constructor and loadSounds() ...

  /**
   * Play a sound effect with volume from settings.
   */
  play(soundId: string, volumeMultiplier = 1): void {
    const effectiveVolume = this.settingsService.getEffectiveEffectsVolume();
    if (effectiveVolume === 0 || !this.audioContext) return;

    const buffer = this.sounds.get(soundId);
    if (!buffer) return;

    try {
      const source = this.audioContext.createBufferSource();
      source.buffer = buffer;

      const gainNode = this.audioContext.createGain();
      gainNode.gain.value = effectiveVolume * volumeMultiplier;

      source.connect(gainNode);
      gainNode.connect(this.audioContext.destination);
      source.start(0);
    } catch (error) {
      console.warn(`Failed to play sound: ${soundId}`, error);
    }
  }

  // Remove individual volume methods - now handled by SettingsService
}
```

---

### Part 4: Event-Driven Statistics System

#### 4.1 Generic Game Event Interface

**New File: `src/main/java/be/lefief/game/core/event/GameEvent.java`**

```java
package be.lefief.game.core.event;

/**
 * Base interface for all game events.
 * Using marker interface allows type-safe event handling.
 */
public interface GameEvent {
    long getTimestamp();
    int getPlayerNumber();
}
```

**New File: `src/main/java/be/lefief/game/core/event/GameEventType.java`**

```java
package be.lefief.game.core.event;

/**
 * Generic event types that apply to any game mode.
 * Game-specific events can extend this concept.
 */
public enum GameEventType {
    RESOURCE_GAINED,
    RESOURCE_SPENT,
    UNIT_CREATED,
    UNIT_DESTROYED,
    STRUCTURE_BUILT,
    DAMAGE_DEALT,
    DAMAGE_RECEIVED,
}
```

#### 4.2 Turrest-Specific Events

**New File: `src/main/java/be/lefief/game/turrest01/event/TurrestEvent.java`**

```java
package be.lefief.game.turrest01.event;

import be.lefief.game.core.event.GameEvent;

/**
 * Base class for Turrest game events.
 */
public abstract class TurrestEvent implements GameEvent {
    protected final long timestamp;
    protected final int playerNumber;

    protected TurrestEvent(int playerNumber) {
        this.timestamp = System.currentTimeMillis();
        this.playerNumber = playerNumber;
    }

    @Override
    public long getTimestamp() { return timestamp; }

    @Override
    public int getPlayerNumber() { return playerNumber; }
}

/**
 * Fired when a creep is killed by towers.
 */
public class CreepKilledEvent extends TurrestEvent {
    private final String creepTypeId;
    private final int goldReward;

    public CreepKilledEvent(int playerNumber, String creepTypeId, int goldReward) {
        super(playerNumber);
        this.creepTypeId = creepTypeId;
        this.goldReward = goldReward;
    }

    public String getCreepTypeId() { return creepTypeId; }
    public int getGoldReward() { return goldReward; }
}

/**
 * Fired when a player sends a creep to opponents.
 */
public class CreepSentEvent extends TurrestEvent {
    private final String creepTypeId;
    private final int goldCost;

    public CreepSentEvent(int playerNumber, String creepTypeId, int goldCost) {
        super(playerNumber);
        this.creepTypeId = creepTypeId;
        this.goldCost = goldCost;
    }

    public String getCreepTypeId() { return creepTypeId; }
    public int getGoldCost() { return goldCost; }
}

/**
 * Fired when a building is constructed.
 */
public class BuildingBuiltEvent extends TurrestEvent {
    private final int buildingTypeId;
    private final int x, y;

    public BuildingBuiltEvent(int playerNumber, int buildingTypeId, int x, int y) {
        super(playerNumber);
        this.buildingTypeId = buildingTypeId;
        this.x = x;
        this.y = y;
    }

    public int getBuildingTypeId() { return buildingTypeId; }
    public int getX() { return x; }
    public int getY() { return y; }
}

/**
 * Fired when a tower is constructed.
 */
public class TowerBuiltEvent extends TurrestEvent {
    private final int towerTypeId;
    private final int x, y;

    public TowerBuiltEvent(int playerNumber, int towerTypeId, int x, int y) {
        super(playerNumber);
        this.towerTypeId = towerTypeId;
        this.x = x;
        this.y = y;
    }

    public int getTowerTypeId() { return towerTypeId; }
    public int getX() { return x; }
    public int getY() { return y; }
}

/**
 * Fired when damage is dealt to a creep.
 */
public class DamageDealtEvent extends TurrestEvent {
    private final int damage;
    private final int towerTypeId;

    public DamageDealtEvent(int playerNumber, int damage, int towerTypeId) {
        super(playerNumber);
        this.damage = damage;
        this.towerTypeId = towerTypeId;
    }

    public int getDamage() { return damage; }
    public int getTowerTypeId() { return towerTypeId; }
}
```

#### 4.3 Event-Driven Stats Collector

**New File: `src/main/java/be/lefief/game/turrest01/stats/GameStats.java`**

```java
package be.lefief.game.turrest01.stats;

import be.lefief.game.turrest01.event.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe game statistics collector.
 * Subscribes to game events and aggregates statistics.
 */
public class GameStats {
    private final Map<Integer, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final long gameStartTime = System.currentTimeMillis();

    /**
     * Process a game event and update stats accordingly.
     * Central entry point for all stat recording.
     */
    public void recordEvent(TurrestEvent event) {
        int player = event.getPlayerNumber();
        PlayerStats stats = playerStats.computeIfAbsent(player, PlayerStats::new);

        if (event instanceof CreepKilledEvent e) {
            stats.creepsKilled.incrementAndGet();
            stats.goldEarned.addAndGet(e.getGoldReward());
        } else if (event instanceof CreepSentEvent e) {
            stats.creepsSent.incrementAndGet();
            stats.goldSpent.addAndGet(e.getGoldCost());
        } else if (event instanceof BuildingBuiltEvent e) {
            stats.buildingsPlaced.incrementAndGet();
        } else if (event instanceof TowerBuiltEvent e) {
            stats.towersPlaced.incrementAndGet();
        } else if (event instanceof DamageDealtEvent e) {
            stats.damageDealt.addAndGet(e.getDamage());
        }
    }

    public PlayerStats getStats(int playerNumber) {
        return playerStats.getOrDefault(playerNumber, new PlayerStats(playerNumber));
    }

    public long getGameDurationMs() {
        return System.currentTimeMillis() - gameStartTime;
    }

    public Map<Integer, PlayerStats> getAllPlayerStats() {
        return Map.copyOf(playerStats);
    }
}

/**
 * Thread-safe per-player statistics.
 * Uses AtomicInteger/AtomicLong for lock-free concurrent updates.
 */
public class PlayerStats {
    private final int playerNumber;

    final AtomicLong goldEarned = new AtomicLong(0);
    final AtomicLong goldSpent = new AtomicLong(0);
    final AtomicInteger creepsKilled = new AtomicInteger(0);
    final AtomicInteger creepsSent = new AtomicInteger(0);
    final AtomicLong damageDealt = new AtomicLong(0);
    final AtomicLong damageTaken = new AtomicLong(0);
    final AtomicInteger buildingsPlaced = new AtomicInteger(0);
    final AtomicInteger towersPlaced = new AtomicInteger(0);

    public PlayerStats(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    // Getters for serialization
    public int getPlayerNumber() { return playerNumber; }
    public long getGoldEarned() { return goldEarned.get(); }
    public long getGoldSpent() { return goldSpent.get(); }
    public int getCreepsKilled() { return creepsKilled.get(); }
    public int getCreepsSent() { return creepsSent.get(); }
    public long getDamageDealt() { return damageDealt.get(); }
    public long getDamageTaken() { return damageTaken.get(); }
    public int getBuildingsPlaced() { return buildingsPlaced.get(); }
    public int getTowersPlaced() { return towersPlaced.get(); }

    /**
     * Convert to serializable map for command response.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "playerNumber", playerNumber,
            "goldEarned", goldEarned.get(),
            "goldSpent", goldSpent.get(),
            "creepsKilled", creepsKilled.get(),
            "creepsSent", creepsSent.get(),
            "damageDealt", damageDealt.get(),
            "damageTaken", damageTaken.get(),
            "buildingsPlaced", buildingsPlaced.get(),
            "towersPlaced", towersPlaced.get()
        );
    }
}
```

#### 4.4 Integrate with Game Mode

**Modify: `TurrestGameMode01.java`**

```java
public class TurrestGameMode01 extends Game<Turrest01Player> {
    // ... existing fields ...
    private final GameStats gameStats = new GameStats();

    /**
     * Record a game event for statistics.
     * Call this from managers instead of scattered record methods.
     */
    public void recordEvent(TurrestEvent event) {
        gameStats.recordEvent(event);
    }

    public GameStats getGameStats() {
        return gameStats;
    }
}
```

**Modify: `CreepManager.java` (example usage)**

```java
// When creep is killed:
game.recordEvent(new CreepKilledEvent(
    playerNumber,
    creep.getType().getId(),
    creep.getType().getKillReward().getGold()
));
```

---

### Part 5: Game Menu Components

#### 5.1 Main Menu Component

**New File: `frontend/src/app/features/game/components/game-menu/game-menu.component.ts`**

```typescript
@Component({
  selector: 'app-game-menu',
  standalone: true,
  imports: [CommonModule, OptionsPanelComponent, StatsPanelComponent],
  template: `
    <div class="menu-overlay" (click)="onOverlayClick($event)">
      <div class="menu-panel" (click)="$event.stopPropagation()">
        <h2>Menu</h2>

        @switch (activeTab()) {
          @case ('main') {
            <div class="menu-buttons">
              <button (click)="onContinue()">Continue</button>
              <button (click)="activeTab.set('options')">Options</button>
              <button (click)="activeTab.set('stats')">Stats</button>
              <button class="quit-btn" (click)="onQuit()">Quit Game</button>
            </div>
          }
          @case ('options') {
            <app-options-panel (back)="activeTab.set('main')"/>
          }
          @case ('stats') {
            <app-stats-panel (back)="activeTab.set('main')"/>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './game-menu.component.scss'
})
export class GameMenuComponent implements OnInit, OnDestroy {
  @Output() continue = new EventEmitter<void>();
  @Output() quit = new EventEmitter<void>();

  private inputService = inject(InputService);

  activeTab = signal<'main' | 'options' | 'stats'>('main');

  ngOnInit(): void {
    // Push modal context to block game inputs
    this.inputService.pushContext(InputContext.MODAL);
  }

  ngOnDestroy(): void {
    this.inputService.popContext(InputContext.MODAL);
  }

  onOverlayClick(event: MouseEvent): void {
    this.onContinue();
  }

  onContinue(): void {
    this.continue.emit();
  }

  onQuit(): void {
    this.quit.emit();
  }
}
```

#### 5.2 Options Panel with Tabs

**New File: `frontend/src/app/features/game/components/game-menu/options-panel.component.ts`**

```typescript
@Component({
  selector: 'app-options-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="options-panel">
      <div class="tabs">
        <button
          [class.active]="activeTab() === 'audio'"
          (click)="activeTab.set('audio')">Audio</button>
        <button
          [class.active]="activeTab() === 'controls'"
          (click)="activeTab.set('controls')">Controls</button>
      </div>

      @switch (activeTab()) {
        @case ('audio') {
          <div class="audio-settings">
            <label>
              <span>Master Volume</span>
              <input type="range" min="0" max="100"
                [value]="masterVolume() * 100"
                (input)="onMasterChange($event)">
              <span>{{ (masterVolume() * 100) | number:'1.0-0' }}%</span>
            </label>

            <label>
              <span>Music Volume</span>
              <input type="range" min="0" max="100"
                [value]="musicVolume() * 100"
                (input)="onMusicChange($event)">
              <span>{{ (musicVolume() * 100) | number:'1.0-0' }}%</span>
            </label>

            <label>
              <span>Effects Volume</span>
              <input type="range" min="0" max="100"
                [value]="effectsVolume() * 100"
                (input)="onEffectsChange($event)">
              <span>{{ (effectsVolume() * 100) | number:'1.0-0' }}%</span>
            </label>

            <label class="checkbox">
              <input type="checkbox" [checked]="muted()" (change)="onMuteToggle()">
              <span>Mute All</span>
            </label>
          </div>
        }
        @case ('controls') {
          <div class="controls-settings">
            <div class="keybindings-list">
              @for (meta of actionRegistry; track meta.action) {
                <div class="keybinding-row">
                  <span class="action-name">{{ meta.displayName }}</span>
                  <button
                    class="key-button"
                    [class.rebinding]="rebindingAction() === meta.action"
                    (click)="startRebind(meta.action)">
                    {{ getBindingDisplay(meta.action) }}
                  </button>
                </div>
              }
            </div>

            <div class="controls-actions">
              <button (click)="resetToDefaults()">Reset to Defaults</button>
              <button (click)="exportKeymap()">Export</button>
              <button (click)="importKeymap()">Import</button>
            </div>
          </div>
        }
      }

      <button class="back-btn" (click)="back.emit()">Back</button>
    </div>
  `
})
export class OptionsPanelComponent {
  @Output() back = new EventEmitter<void>();

  private settingsService = inject(SettingsService);
  private inputService = inject(InputService);

  activeTab = signal<'audio' | 'controls'>('audio');
  rebindingAction = signal<InputAction | null>(null);

  // Expose settings as signals
  masterVolume = this.settingsService.masterVolume;
  musicVolume = this.settingsService.musicVolume;
  effectsVolume = this.settingsService.effectsVolume;
  muted = this.settingsService.muted;

  actionRegistry = INPUT_ACTION_REGISTRY;

  onMasterChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber / 100;
    this.settingsService.setAudioSettings({ masterVolume: value });
  }

  onMusicChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber / 100;
    this.settingsService.setAudioSettings({ musicVolume: value });
  }

  onEffectsChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber / 100;
    this.settingsService.setAudioSettings({ effectsVolume: value });
  }

  onMuteToggle(): void {
    this.settingsService.setAudioSettings({ muted: !this.muted() });
  }

  getBindingDisplay(action: InputAction): string {
    const bindings = this.settingsService.getKeyBindings();
    const binding = bindings.get(action) ??
      INPUT_ACTION_REGISTRY.find(m => m.action === action)?.defaultBinding;

    if (!binding) return '???';

    let display = '';
    if (binding.ctrl) display += 'Ctrl+';
    if (binding.shift) display += 'Shift+';
    if (binding.alt) display += 'Alt+';
    display += binding.key;

    return display;
  }

  async startRebind(action: InputAction): Promise<void> {
    this.rebindingAction.set(action);
    const binding = await this.inputService.startRebinding(action);
    this.settingsService.setKeyBinding(action, binding);
    this.rebindingAction.set(null);
  }

  resetToDefaults(): void {
    this.settingsService.resetKeyBindingsToDefaults();
  }

  exportKeymap(): void {
    const json = this.settingsService.exportKeyBindings();
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'turrest-keybindings.json';
    a.click();
    URL.revokeObjectURL(url);
  }

  async importKeymap(): Promise<void> {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;

      const text = await file.text();
      const error = this.settingsService.importKeyBindings(text);
      if (error) {
        alert(`Import failed: ${error}`);
      }
    };
    input.click();
  }
}
```

---

### Part 6: Stats Request/Response Commands

**New File: `src/main/java/be/lefief/game/turrest01/commands/GetStatsCommand.java`**

```java
public class GetStatsCommand extends ClientToServerCommand {
    public static final ServerSocketSubject SUBJECT = ServerSocketSubject.GAME;
    public static final String TOPIC = "GET_STATS";
}
```

**New File: `src/main/java/be/lefief/game/turrest01/commands/StatsResponseCommand.java`**

```java
public class StatsResponseCommand extends ServerToClientCommand {
    public StatsResponseCommand(GameStats stats) {
        super(ClientSocketSubject.GAME, "STATS");

        Map<String, Object> data = new HashMap<>();
        data.put("gameDurationMs", stats.getGameDurationMs());

        List<Map<String, Object>> playerStatsList = new ArrayList<>();
        for (PlayerStats ps : stats.getAllPlayerStats().values()) {
            playerStatsList.add(ps.toMap());
        }
        data.put("players", playerStatsList);

        this.data = data;
    }
}
```

---

### Part 7: Files to Create/Modify

#### Frontend (TypeScript)

| File | Action | Purpose |
|------|--------|---------|
| `input-action.model.ts` | CREATE | Type-safe action enums with metadata |
| `input.service.ts` | CREATE | Context-aware keyboard handling |
| `settings.service.ts` | CREATE | Versioned settings persistence |
| `audio.service.ts` | MODIFY | Use SettingsService for volumes |
| `game-menu.component.ts` | CREATE | Main menu overlay |
| `options-panel.component.ts` | CREATE | Audio & controls with tabs |
| `stats-panel.component.ts` | CREATE | Game statistics display |
| `game.component.ts` | MODIFY | Integrate InputService |
| `services/index.ts` | MODIFY | Export new services |

#### Backend (Java)

| File | Action | Purpose |
|------|--------|---------|
| `GameEvent.java` | CREATE | Base event interface |
| `TurrestEvent.java` | CREATE | Turrest event base class |
| `CreepKilledEvent.java` | CREATE | Creep kill event |
| `CreepSentEvent.java` | CREATE | Creep send event |
| `BuildingBuiltEvent.java` | CREATE | Building placement event |
| `TowerBuiltEvent.java` | CREATE | Tower placement event |
| `DamageDealtEvent.java` | CREATE | Damage dealt event |
| `GameStats.java` | CREATE | Event-driven stats collector |
| `PlayerStats.java` | CREATE | Thread-safe player stats |
| `GetStatsCommand.java` | CREATE | Stats request command |
| `StatsResponseCommand.java` | CREATE | Stats response command |
| `GetStatsHandler.java` | CREATE | Handle stats requests |
| `TurrestGameMode01.java` | MODIFY | Add GameStats, recordEvent() |
| `CreepManager.java` | MODIFY | Fire events instead of direct recording |
| `Turrest01GameHandler.java` | MODIFY | Fire events for builds |

---

### Part 8: Implementation Order

1. **Input Action Model** (30 min)
   - Create InputAction enum with all actions
   - Define InputContext for priority handling
   - Create INPUT_ACTION_REGISTRY with metadata

2. **Settings Service** (1 hr)
   - Implement versioned schema
   - Add audio settings
   - Add key bindings storage
   - Implement import/export with validation

3. **Input Service** (1 hr)
   - Implement context stack
   - Implement key matching with modifiers
   - Implement rebinding mode
   - Test context priority (Escape in menu vs game)

4. **Update AudioService** (30 min)
   - Remove internal volume state
   - Use SettingsService for volumes
   - Test volume changes

5. **Game Menu UI** (2 hr)
   - Create GameMenuComponent with overlay
   - Create OptionsPanelComponent with tabs
   - Implement audio sliders
   - Implement keybinding list with rebind

6. **Backend Events** (1 hr)
   - Create event classes
   - Implement GameStats collector
   - Update CreepManager to fire events
   - Update Turrest01GameHandler to fire events

7. **Stats Commands** (30 min)
   - Create GetStatsCommand/Handler
   - Create StatsResponseCommand
   - Test stats request/response

8. **Stats Panel** (1 hr)
   - Create StatsPanelComponent
   - Request stats from server
   - Display with formatting

---

### Key Improvements Summary

| Issue | Before | After |
|-------|--------|-------|
| Magic strings for actions | `'building.lumbercamp'` | `InputAction.BUILD_LUMBERCAMP` enum |
| Conflicting keys (Escape) | Both actions fire | Context priority (PLACEMENT > GAME) |
| No modifier support | Single keys only | `{ key: 'S', ctrl: true }` |
| Volume state duplication | AudioService + SettingsService | SettingsService is source of truth |
| No schema versioning | Breaking on changes | Version field + migrations |
| Scattered stat recording | recordCreepKill(), recordBuildingPlaced()... | Single recordEvent(TurrestEvent) |
| Non-thread-safe stats | int fields | AtomicInteger/AtomicLong |
| Game-specific stats | Tied to TurrestGameMode01 | Generic GameEvent interface |

---

### Testing Checklist

- [ ] Escape opens menu in game context
- [ ] Escape cancels placement in placement context
- [ ] Escape closes menu in modal context
- [ ] Ctrl+key bindings work correctly
- [ ] Volume sliders update in real-time
- [ ] Mute toggle works
- [ ] Settings persist after page refresh
- [ ] Settings migrate from old format
- [ ] Key rebinding captures next key
- [ ] Export creates valid JSON file
- [ ] Import validates and applies bindings
- [ ] Invalid JSON shows error message
- [ ] Stats update in real-time
- [ ] Stats are thread-safe under load
- [ ] Quit button returns to lobby

---

## Post-Implementation Investigation (2025-12-29)

### Issue 1: Stats Not Showing (Only Game Duration Visible)

**Root Cause Analysis:**

The event classes (`CreepKilledEvent`, `BuildingBuiltEvent`, etc.) and `GameStats` collector were created, but **no code ever calls `game.recordEvent()`**. The stats remain empty because events are never fired.

**Investigation Points:**
1. `CreepManager.java` lines 170-192 - creep kill logic exists but no event recorded
2. `Turrest01GameHandler.java` - building/tower placement logic exists but no event recorded
3. `GameStats.recordEvent()` is never invoked anywhere in the codebase

**Fix Required:**

Add `game.recordEvent()` calls at each stat-relevant location:

```java
// CreepManager.java - When creep is killed (around line 185):
} else if (creep.isDead()) {
    int playerNum = creep.getOwnerPlayerNumber();
    Turrest01Player player = game.getPlayerByNumber().get(playerNum);
    if (player != null) {
        creep.getType().getKillReward().apply(player);

        // ADD: Record kill event for stats
        game.recordEvent(new CreepKilledEvent(
            playerNum,
            creep.getType().getId(),
            creep.getType().getKillReward().getGold()
        ));
        // ... rest of existing code
    }
}

// CreepManager.java - When creep reaches castle (around line 149):
Integer senderPlayerNumber = creep.getSpawnedByPlayer();
if (senderPlayerNumber != null) {
    // ADD: Record damage taken by defending player
    game.getGameStats().recordDamageTaken(creep.getOwnerPlayerNumber(), damage);
    // ... existing reward code
}

// Turrest01GameHandler.java - handlePlaceBuilding (around line 136):
tile.setStructure(new TurrestBuilding(buildingDef, player.getPlayerNumber()));
// ADD: Record building event
turrestGame.recordEvent(new BuildingBuiltEvent(
    player.getPlayerNumber(),
    buildingDef.getId(),
    x, y
));

// Turrest01GameHandler.java - handlePlaceTower (around line 244):
turrestGame.getTowerManager().addTower(tower);
// ADD: Record tower event
turrestGame.recordEvent(new TowerBuiltEvent(
    player.getPlayerNumber(),
    towerDef.getId(),
    x, y
));

// Turrest01GameHandler.java - handleSendCreep (around line 325):
sendCost.apply(player);
// ADD: Record creep sent event
turrestGame.recordEvent(new CreepSentEvent(
    player.getPlayerNumber(),
    creepType.getId(),
    sendCost.getGold()
));

// TowerManager.java - When tower deals damage:
// ADD: Record damage dealt event
game.recordEvent(new DamageDealtEvent(
    tower.getOwnerPlayerNumber(),
    damage,
    tower.getDefinition().getId()
));
```

**Files to Modify:**
- `CreepManager.java` - Add CreepKilledEvent, damageTaken recording
- `Turrest01GameHandler.java` - Add BuildingBuiltEvent, TowerBuiltEvent, CreepSentEvent
- `TowerManager.java` - Add DamageDealtEvent

---

### Issue 2: Creep Diagonal Movement Through Grass (Desync)

**Root Cause Analysis:**

The creep movement system uses client-side interpolation that doesn't follow the path:

1. **Server** (`Creep.java` lines 40-75): Moves creep along discrete path points, following road tiles
2. **Server** sends position update every tick (200ms) with new `(x, y)` coordinates
3. **Client** (`game.component.ts` lines 786-804): Receives `(targetX, targetY)` and interpolates **directly** toward it
4. **Problem**: If the client is still interpolating from position A when server sends position C (having passed through B), the client draws a **straight line from A to C**, cutting diagonally through grass/walls

**Visual Example:**
```
Server path:     A → B → C (follows road, turns corner at B)
Client receives: A, then C (missed B because still interpolating)
Client draws:    A ----→ C (diagonal shortcut through grass)
```

**Contributing Factors:**
- 200ms tick rate means creep can move ~0.4 tiles per update at speed 2.0
- Client interpolation is slower than server movement in some cases
- No path information sent to client - only current position

**Proposed Solutions (Pick One):**

#### Option A: Snap When Distance Too Large (Quick Fix)
```typescript
// game.component.ts - updateCreepPositions()
private updateCreepPositions(deltaTime: number): void {
  for (const creep of this.creeps.values()) {
    const dx = creep.targetX - creep.x;
    const dy = creep.targetY - creep.y;
    const distance = Math.sqrt(dx * dx + dy * dy);

    // NEW: If too far behind, snap to prevent diagonal shortcuts
    if (distance > 1.5) {  // More than 1.5 tiles behind
      creep.x = creep.targetX;
      creep.y = creep.targetY;
    } else if (distance > 0.01) {
      const moveDistance = creep.speed * deltaTime;
      const ratio = Math.min(moveDistance / distance, 1);
      creep.x += dx * ratio;
      creep.y += dy * ratio;
    } else {
      creep.x = creep.targetX;
      creep.y = creep.targetY;
    }
  }
}
```
**Pros:** Simple, minimal changes
**Cons:** Visual "teleporting" when catching up

#### Option B: Send Path Waypoints (Better Visual Quality)
```java
// Backend - BatchedCreepUpdateCommand.java
// Include next 2-3 path waypoints with each update
data.put("pathPoints", getNextWaypoints(creep, 3));
```

```typescript
// Frontend - Store path queue per creep
interface Creep {
  // ... existing fields
  pathQueue: {x: number, y: number}[];  // Waypoints to follow
}

// Interpolate through waypoints in order
private updateCreepPositions(deltaTime: number): void {
  for (const creep of this.creeps.values()) {
    while (creep.pathQueue.length > 0) {
      const next = creep.pathQueue[0];
      const dx = next.x - creep.x;
      const dy = next.y - creep.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance > 0.01) {
        const moveDistance = creep.speed * deltaTime;
        if (moveDistance >= distance) {
          creep.x = next.x;
          creep.y = next.y;
          creep.pathQueue.shift();  // Remove reached waypoint
          deltaTime -= distance / creep.speed;  // Continue with remaining time
        } else {
          creep.x += dx * (moveDistance / distance);
          creep.y += dy * (moveDistance / distance);
          break;
        }
      } else {
        creep.pathQueue.shift();
      }
    }
  }
}
```
**Pros:** Smooth, accurate path following
**Cons:** More network data, more complex client logic

#### Option C: Increase Tick Rate (Simpler Server Change)
```java
// TurrestGameMode01.java
private static final int TICK_RATE_MS = 100;  // Was 200ms, now 100ms (10 Hz)
```
**Pros:** Smaller position jumps, less visual desync
**Cons:** 2x network traffic, 2x server load

**Recommended Approach:** Start with **Option A** (snap threshold) for quick fix, then implement **Option B** (path waypoints) for polish if needed.

---

### Implementation Priority

| Fix | Effort | Impact | Priority |
|-----|--------|--------|----------|
| Add recordEvent() calls | 30 min | High - Stats completely broken | **P0** |
| Snap threshold for creeps | 15 min | Medium - Visual issue | **P1** |
| Path waypoints for creeps | 2 hr | Low - Polish | **P2** |

---

### Testing After Fixes

- [ ] Build a lumbercamp → Stats shows buildingsPlaced: 1
- [ ] Kill a creep → Stats shows creepsKilled: 1, goldEarned: X
- [ ] Send a creep → Stats shows creepsSent: 1, goldSpent: X
- [ ] Build a tower → Stats shows towersPlaced: 1
- [ ] Creep follows path around corners without diagonal shortcuts
- [ ] Creep position recovers smoothly after network lag

