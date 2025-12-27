import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Tile, TerrainType, PlayerResources, BuildingDefinition, BUILDING_DEFINITIONS } from '../../../../shared/models';

@Component({
  selector: 'app-action-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pane-section actions-pane">
      <h4>{{ placementMode ? 'Placing: ' + placementMode.name : 'Buildings' }}</h4>

      @if (placementMode) {
        <div class="placement-info">
          <p>Click on the map to place {{ placementMode.name }}</p>
          <p class="hint">Press ESC or click Cancel to exit placement mode</p>
          <button class="cancel-btn" (click)="cancelPlacement()">Cancel</button>
        </div>
      } @else {
        <div class="action-buttons">
          @for (building of buildings; track building.id) {
            <button
              class="action-btn"
              [class.disabled]="!canAfford(building)"
              [disabled]="!canAfford(building)"
              (click)="selectBuilding(building)"
              [title]="getBuildingTooltip(building)"
            >
              <span class="action-icon">{{ building.icon }}</span>
              <span class="building-name">{{ building.name }}</span>
              <div class="cost-row">
                <span class="cost wood">🪵{{ building.cost.wood }}</span>
                <span class="cost stone">🪨{{ building.cost.stone }}</span>
                <span class="cost gold">🪙{{ building.cost.gold }}</span>
              </div>
            </button>
          }
        </div>
      }
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
      gap: 0.2rem;
      padding: 0.5rem 0.75rem;
      background: #1a1a2e;
      border: 1px solid #444;
      border-radius: 8px;
      color: #fff;
      cursor: pointer;
      transition: all 0.2s;
      min-width: 90px;
    }

    .action-btn:hover:not(:disabled) {
      background: #2a2a4e;
      border-color: #00d9ff;
    }

    .action-btn:disabled,
    .action-btn.disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .action-icon {
      font-size: 1.5rem;
    }

    .building-name {
      font-size: 0.7rem;
      white-space: nowrap;
    }

    .cost-row {
      display: flex;
      gap: 0.3rem;
      font-size: 0.65rem;
    }

    .cost {
      padding: 0.1rem 0.2rem;
      border-radius: 3px;
      background: rgba(0,0,0,0.3);
    }

    .cost.wood { color: #8B4513; }
    .cost.stone { color: #A9A9A9; }
    .cost.gold { color: #FFD700; }

    .placement-info {
      text-align: center;
      color: #aaa;
    }

    .placement-info p {
      margin: 0.5rem 0;
    }

    .placement-info .hint {
      font-size: 0.8rem;
      font-style: italic;
      color: #666;
    }

    .cancel-btn {
      margin-top: 0.5rem;
      padding: 0.5rem 1rem;
      background: #333;
      border: 1px solid #555;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
    }

    .cancel-btn:hover {
      background: #444;
    }
  `]
})
export class ActionPanelComponent {
  @Input() selectedTile: Tile | null = null;
  @Input() resources: PlayerResources = { wood: 100, stone: 100, gold: 100 };
  @Input() placementMode: BuildingDefinition | null = null;

  @Output() actionTriggered = new EventEmitter<string>();
  @Output() buildingSelected = new EventEmitter<BuildingDefinition>();
  @Output() placementCancelled = new EventEmitter<void>();

  buildings = BUILDING_DEFINITIONS;

  canAfford(building: BuildingDefinition): boolean {
    return this.resources.wood >= building.cost.wood &&
           this.resources.stone >= building.cost.stone &&
           this.resources.gold >= building.cost.gold;
  }

  selectBuilding(building: BuildingDefinition): void {
    if (this.canAfford(building)) {
      this.buildingSelected.emit(building);
    }
  }

  cancelPlacement(): void {
    this.placementCancelled.emit();
  }

  getBuildingTooltip(building: BuildingDefinition): string {
    const terrainNames = building.allowedTerrains.map(t => TerrainType[t]).join(', ');
    return `${building.name}\nCost: ${building.cost.wood} Wood, ${building.cost.stone} Stone, ${building.cost.gold} Gold\nCan build on: ${terrainNames}`;
  }
}
