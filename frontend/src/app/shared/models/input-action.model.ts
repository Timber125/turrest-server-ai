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

  // Buildings (mapped to BuildingDefinition IDs)
  BUILD_LUMBERCAMP = 'building.1',
  BUILD_STONE_QUARRY = 'building.2',
  BUILD_GOLD_MINE = 'building.3',

  // Towers
  TOWER_BASIC = 'tower.1',

  // Creeps (1-9 reserved for creep hotkeys)
  SEND_CREEP_1 = 'creep.slot1',
  SEND_CREEP_2 = 'creep.slot2',
  SEND_CREEP_3 = 'creep.slot3',
}

/**
 * Input context determines which actions are active.
 * Lower values = higher priority (consume keys first).
 */
export enum InputContext {
  TEXT_INPUT = 0,    // Typing in input fields - blocks all game keys
  MODAL = 1,         // Menu/modal open - limited keys work
  PLACEMENT = 2,     // Placing building/tower - cancel key active
  GAME = 3,          // Normal gameplay - all keys active
}

/**
 * Key binding with optional modifier keys.
 */
export interface KeyBinding {
  key: string;           // e.g., 'Q', 'Escape', 'F1'
  ctrl?: boolean;
  shift?: boolean;
  alt?: boolean;
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

/**
 * Registry of all input actions with their metadata.
 * Order matters for conflict resolution - first match wins.
 */
export const INPUT_ACTION_REGISTRY: InputActionMeta[] = [
  // Menu actions - work in multiple contexts
  {
    action: InputAction.MENU_TOGGLE,
    displayName: 'Toggle Menu',
    category: 'menu',
    defaultBinding: { key: 'Escape' },
    contexts: [InputContext.GAME, InputContext.MODAL],
  },
  {
    action: InputAction.SELECTION_CANCEL,
    displayName: 'Cancel Selection',
    category: 'menu',
    defaultBinding: { key: 'Escape' },
    contexts: [InputContext.PLACEMENT],  // Only in placement mode - takes priority
  },
  {
    action: InputAction.MENU_STATS,
    displayName: 'Quick Stats',
    category: 'menu',
    defaultBinding: { key: 'Tab' },
    contexts: [InputContext.GAME],
  },

  // Camera actions
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

  // Building actions
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

  // Tower actions
  {
    action: InputAction.TOWER_BASIC,
    displayName: 'Build Basic Tower',
    category: 'tower',
    defaultBinding: { key: 'T' },
    contexts: [InputContext.GAME],
  },

  // Creep actions
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
  {
    action: InputAction.SEND_CREEP_3,
    displayName: 'Send Creep (Slot 3)',
    category: 'creep',
    defaultBinding: { key: '3' },
    contexts: [InputContext.GAME],
  },
];

/**
 * Get default binding for an action.
 */
export function getDefaultBinding(action: InputAction): KeyBinding | undefined {
  return INPUT_ACTION_REGISTRY.find(m => m.action === action)?.defaultBinding;
}

/**
 * Get action metadata.
 */
export function getActionMeta(action: InputAction): InputActionMeta | undefined {
  return INPUT_ACTION_REGISTRY.find(m => m.action === action);
}

/**
 * Format a key binding for display (e.g., "Ctrl+S").
 */
export function formatKeyBinding(binding: KeyBinding): string {
  let display = '';
  if (binding.ctrl) display += 'Ctrl+';
  if (binding.shift) display += 'Shift+';
  if (binding.alt) display += 'Alt+';
  display += binding.key.length === 1 ? binding.key.toUpperCase() : binding.key;
  return display;
}
