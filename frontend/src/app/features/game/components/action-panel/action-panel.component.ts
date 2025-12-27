import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Tile, TerrainType } from '../../../../shared/models';

@Component({
    selector: 'app-action-panel',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="pane-section actions-pane">
      <h4>Actions</h4>
      <div class="action-buttons">
        <button
          class="action-btn"
          [disabled]="!canBuildOnTile(selectedTile)"
          (click)="handleAction('createTurret')"
        >
          <span class="action-icon">🗼</span>
          <span>Create Turret</span>
        </button>
        <button class="action-btn" [disabled]="!selectedTile">
          <span class="action-icon">🏗️</span>
          <span>Build Wall</span>
        </button>
        <button class="action-btn" [disabled]="!selectedTile">
          <span class="action-icon">⛏️</span>
          <span>Demolish</span>
        </button>
      </div>
    </div>
  `,
    styles: [`
    .pane-section {
      background: #0f0f23;
      border-radius: 8px;
      border: 1px solid #333;
      padding: 0.75rem;
      overflow: hidden;
      height: 100%;
      box-sizing: border-box;
    }

    h4 {
      color: #00d9ff;
      margin: 0 0 0.5rem 0;
      font-size: 0.9rem;
      border-bottom: 1px solid #333;
      padding-bottom: 0.4rem;
    }

    .action-buttons {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.3rem;
      padding: 0.75rem 1rem;
      background: #1a1a2e;
      border: 1px solid #444;
      border-radius: 8px;
      color: #fff;
      cursor: pointer;
      transition: all 0.2s;
      min-width: 80px;
    }

    .action-btn:hover:not(:disabled) {
      background: #2a2a4e;
      border-color: #00d9ff;
    }

    .action-btn:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .action-icon {
      font-size: 1.5rem;
    }

    .action-btn span:last-child {
      font-size: 0.75rem;
    }
  `]
})
export class ActionPanelComponent {
    @Input() selectedTile: Tile | null = null;
    @Output() actionTriggered = new EventEmitter<string>();

    canBuildOnTile(tile: Tile | null): boolean {
        if (!tile) return false;
        // Can only build on grass or dirt
        return tile.terrainType === TerrainType.GRASS || tile.terrainType === TerrainType.DIRT;
    }

    handleAction(actionName: string): void {
        if (this.selectedTile) {
            this.actionTriggered.emit(actionName);
        }
    }
}
