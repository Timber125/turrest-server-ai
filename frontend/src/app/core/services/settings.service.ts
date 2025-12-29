import { Injectable, signal, computed } from '@angular/core';
import { InputAction, KeyBinding } from '../../shared/models';

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

const DEFAULT_AUDIO: AudioSettings = {
  masterVolume: 0.5,
  musicVolume: 0.3,
  effectsVolume: 0.5,
  muted: false,
};

const DEFAULT_SETTINGS: SettingsSchema = {
  version: SETTINGS_VERSION,
  audio: { ...DEFAULT_AUDIO },
  keyBindings: {},  // Empty = use defaults from registry
};

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly STORAGE_KEY = 'turrest_settings';

  // Reactive settings state
  private settings = signal<SettingsSchema>({ ...DEFAULT_SETTINGS });

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
   * Get binding for a specific action.
   */
  getKeyBinding(action: InputAction): KeyBinding | undefined {
    return this.settings().keyBindings[action];
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
   * Set master volume (0-1).
   */
  setMasterVolume(volume: number): void {
    this.setAudioSettings({ masterVolume: Math.max(0, Math.min(1, volume)) });
  }

  /**
   * Set music volume (0-1).
   */
  setMusicVolume(volume: number): void {
    this.setAudioSettings({ musicVolume: Math.max(0, Math.min(1, volume)) });
  }

  /**
   * Set effects volume (0-1).
   */
  setEffectsVolume(volume: number): void {
    this.setAudioSettings({ effectsVolume: Math.max(0, Math.min(1, volume)) });
  }

  /**
   * Toggle mute state.
   */
  toggleMute(): void {
    this.setAudioSettings({ muted: !this.muted() });
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
   * Reset audio to defaults.
   */
  resetAudioToDefaults(): void {
    this.settings.update(s => ({
      ...s,
      audio: { ...DEFAULT_AUDIO },
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
      if (typeof bindings !== 'object' || bindings === null) {
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
        keyBindings: bindings as Record<string, KeyBinding>,
      }));
      this.saveSettings();
      return null;
    } catch {
      return 'Invalid JSON format';
    }
  }

  private loadSettings(): void {
    if (typeof localStorage === 'undefined') return;

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
    if (typeof localStorage === 'undefined') return;
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

    // Migration from v0 (no version field - old format)
    if (version === 0) {
      return {
        version: SETTINGS_VERSION,
        audio: {
          masterVolume: settings.masterVolume ?? DEFAULT_AUDIO.masterVolume,
          musicVolume: settings.musicVolume ?? DEFAULT_AUDIO.musicVolume,
          effectsVolume: settings.effectsVolume ?? DEFAULT_AUDIO.effectsVolume,
          muted: settings.muted ?? false,
        },
        keyBindings: settings.keymap ?? settings.keyBindings ?? {},
      };
    }

    // Future migrations go here
    // if (version === 1) { ... migrate to v2 ... }

    // Unknown version - use defaults
    return { ...DEFAULT_SETTINGS };
  }

  private isValidBinding(binding: any): binding is KeyBinding {
    return (
      typeof binding === 'object' &&
      binding !== null &&
      typeof binding.key === 'string' &&
      (binding.ctrl === undefined || typeof binding.ctrl === 'boolean') &&
      (binding.shift === undefined || typeof binding.shift === 'boolean') &&
      (binding.alt === undefined || typeof binding.alt === 'boolean')
    );
  }
}
