import { Component, Output, EventEmitter, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InputService, SettingsService } from '../../../../core/services';
import { InputAction, KeyBinding, INPUT_ACTION_REGISTRY, getDefaultBinding } from '../../../../shared/models';

@Component({
  selector: 'app-options-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="options-panel">
      <h2>Options</h2>

      <div class="tabs">
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'audio'"
          (click)="activeTab.set('audio')">
          🔊 Audio
        </button>
        <button
          class="tab-btn"
          [class.active]="activeTab() === 'controls'"
          (click)="activeTab.set('controls')">
          ⌨ Controls
        </button>
      </div>

      <div class="tab-content">
        @switch (activeTab()) {
          @case ('audio') {
            <div class="audio-settings">
              <div class="volume-control">
                <label>
                  <span class="label-text">Master Volume</span>
                  <div class="slider-row">
                    <input
                      type="range"
                      min="0"
                      max="100"
                      [value]="masterVolume() * 100"
                      (input)="onMasterChange($event)">
                    <span class="volume-value">{{ (masterVolume() * 100) | number:'1.0-0' }}%</span>
                  </div>
                </label>
              </div>

              <div class="volume-control">
                <label>
                  <span class="label-text">Music Volume</span>
                  <div class="slider-row">
                    <input
                      type="range"
                      min="0"
                      max="100"
                      [value]="musicVolume() * 100"
                      (input)="onMusicChange($event)">
                    <span class="volume-value">{{ (musicVolume() * 100) | number:'1.0-0' }}%</span>
                  </div>
                </label>
              </div>

              <div class="volume-control">
                <label>
                  <span class="label-text">Effects Volume</span>
                  <div class="slider-row">
                    <input
                      type="range"
                      min="0"
                      max="100"
                      [value]="effectsVolume() * 100"
                      (input)="onEffectsChange($event)">
                    <span class="volume-value">{{ (effectsVolume() * 100) | number:'1.0-0' }}%</span>
                  </div>
                </label>
              </div>

              <label class="mute-control">
                <input
                  type="checkbox"
                  [checked]="muted()"
                  (change)="onMuteToggle()">
                <span>Mute All Audio</span>
              </label>

              <button class="reset-btn" (click)="resetAudio()">
                Reset Audio to Defaults
              </button>
            </div>
          }
          @case ('controls') {
            <div class="controls-settings">
              @if (rebindingAction()) {
                <div class="rebind-overlay">
                  <p>Press any key to bind...</p>
                  <button class="cancel-btn" (click)="cancelRebind()">Cancel</button>
                </div>
              }

              <div class="keybindings-list">
                @for (category of categories; track category) {
                  <div class="category-section">
                    <h4>{{ category | titlecase }}</h4>
                    @for (meta of getActionsForCategory(category); track meta.action) {
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
                }
              </div>

              <div class="controls-actions">
                <button class="action-btn" (click)="resetToDefaults()">Reset to Defaults</button>
                <button class="action-btn" (click)="exportKeymap()">Export</button>
                <button class="action-btn" (click)="importKeymap()">Import</button>
              </div>

              @if (importError()) {
                <div class="error-message">{{ importError() }}</div>
              }
            </div>
          }
        }
      </div>

      <button class="back-btn" (click)="back.emit()">
        ← Back
      </button>
    </div>
  `,
  styles: [`
    .options-panel {
      min-width: 400px;
    }

    h2 {
      color: #00d9ff;
      margin: 0 0 1rem;
      text-align: center;
      font-size: 1.3rem;
    }

    h4 {
      color: #888;
      margin: 0.75rem 0 0.5rem;
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .tabs {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .tab-btn {
      flex: 1;
      padding: 0.6rem;
      background: #0f0f23;
      border: 1px solid #333;
      color: #aaa;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .tab-btn:hover {
      border-color: #555;
      color: #fff;
    }

    .tab-btn.active {
      background: #1f1f3f;
      border-color: #00d9ff;
      color: #fff;
    }

    .tab-content {
      min-height: 250px;
    }

    /* Audio Settings */
    .audio-settings {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .volume-control label {
      display: block;
    }

    .label-text {
      display: block;
      color: #fff;
      margin-bottom: 0.5rem;
      font-size: 0.9rem;
    }

    .slider-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    input[type="range"] {
      flex: 1;
      height: 6px;
      background: #333;
      border-radius: 3px;
      appearance: none;
      cursor: pointer;
    }

    input[type="range"]::-webkit-slider-thumb {
      appearance: none;
      width: 16px;
      height: 16px;
      background: #00d9ff;
      border-radius: 50%;
      cursor: pointer;
    }

    input[type="range"]::-moz-range-thumb {
      width: 16px;
      height: 16px;
      background: #00d9ff;
      border-radius: 50%;
      cursor: pointer;
      border: none;
    }

    .volume-value {
      color: #00d9ff;
      font-weight: bold;
      width: 45px;
      text-align: right;
      font-size: 0.9rem;
    }

    .mute-control {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: #fff;
      cursor: pointer;
      padding: 0.5rem 0;
    }

    .mute-control input[type="checkbox"] {
      width: 18px;
      height: 18px;
      cursor: pointer;
    }

    .reset-btn {
      padding: 0.5rem 1rem;
      background: transparent;
      border: 1px solid #666;
      color: #aaa;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      margin-top: 0.5rem;
    }

    .reset-btn:hover {
      border-color: #888;
      color: #fff;
    }

    /* Controls Settings */
    .controls-settings {
      position: relative;
    }

    .rebind-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.9);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      z-index: 10;
      border-radius: 8px;
    }

    .rebind-overlay p {
      color: #00d9ff;
      font-size: 1.2rem;
    }

    .cancel-btn {
      padding: 0.5rem 1.5rem;
      background: transparent;
      border: 1px solid #666;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
    }

    .keybindings-list {
      max-height: 200px;
      overflow-y: auto;
      margin-bottom: 1rem;
    }

    .category-section {
      margin-bottom: 0.5rem;
    }

    .keybinding-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.4rem 0.5rem;
      background: #0f0f23;
      border-radius: 4px;
      margin-bottom: 4px;
    }

    .action-name {
      color: #ccc;
      font-size: 0.85rem;
    }

    .key-button {
      padding: 0.3rem 0.6rem;
      min-width: 80px;
      background: #1f1f3f;
      border: 1px solid #444;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.8rem;
      text-align: center;
    }

    .key-button:hover {
      border-color: #00d9ff;
    }

    .key-button.rebinding {
      background: #00d9ff;
      color: #000;
      animation: pulse 1s infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.7; }
    }

    .controls-actions {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .action-btn {
      flex: 1;
      min-width: 100px;
      padding: 0.5rem;
      background: transparent;
      border: 1px solid #555;
      color: #aaa;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
    }

    .action-btn:hover {
      border-color: #00d9ff;
      color: #fff;
    }

    .error-message {
      margin-top: 0.75rem;
      padding: 0.5rem;
      background: rgba(255, 68, 68, 0.2);
      border: 1px solid #ff4444;
      border-radius: 4px;
      color: #ff6666;
      font-size: 0.85rem;
    }

    /* Back Button */
    .back-btn {
      width: 100%;
      padding: 0.75rem;
      background: transparent;
      border: 1px solid #444;
      color: #fff;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.9rem;
      margin-top: 1rem;
    }

    .back-btn:hover {
      background: #1f1f3f;
      border-color: #666;
    }
  `]
})
export class OptionsPanelComponent {
  @Output() back = new EventEmitter<void>();

  private readonly settingsService = inject(SettingsService);
  private readonly inputService = inject(InputService);

  activeTab = signal<'audio' | 'controls'>('audio');
  rebindingAction = signal<InputAction | null>(null);
  importError = signal<string | null>(null);

  // Expose settings as signals
  masterVolume = this.settingsService.masterVolume;
  musicVolume = this.settingsService.musicVolume;
  effectsVolume = this.settingsService.effectsVolume;
  muted = this.settingsService.muted;

  // Unique categories from registry
  categories = [...new Set(INPUT_ACTION_REGISTRY.map(m => m.category))];

  getActionsForCategory(category: string) {
    return INPUT_ACTION_REGISTRY.filter(m => m.category === category);
  }

  onMasterChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber / 100;
    this.settingsService.setMasterVolume(value);
  }

  onMusicChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber / 100;
    this.settingsService.setMusicVolume(value);
  }

  onEffectsChange(event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber / 100;
    this.settingsService.setEffectsVolume(value);
  }

  onMuteToggle(): void {
    this.settingsService.toggleMute();
  }

  resetAudio(): void {
    this.settingsService.resetAudioToDefaults();
  }

  getBindingDisplay(action: InputAction): string {
    const customBinding = this.settingsService.getKeyBinding(action);
    const binding = customBinding ?? getDefaultBinding(action);

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
    this.importError.set(null);

    try {
      const binding = await this.inputService.startRebinding(action);
      this.settingsService.setKeyBinding(action, binding);
    } catch {
      // Rebinding was cancelled
    }
    this.rebindingAction.set(null);
  }

  cancelRebind(): void {
    this.inputService.cancelRebinding();
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

      try {
        const text = await file.text();
        const error = this.settingsService.importKeyBindings(text);
        if (error) {
          this.importError.set(error);
        } else {
          this.importError.set(null);
        }
      } catch {
        this.importError.set('Failed to read file');
      }
    };

    input.click();
  }
}
