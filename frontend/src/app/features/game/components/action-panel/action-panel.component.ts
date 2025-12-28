import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Tile, TerrainType, PlayerResources, BuildingDefinition, BUILDING_DEFINITIONS, TowerDefinition, TOWER_DEFINITIONS } from '../../../../shared/models';

@Component({
  selector: 'app-action-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pane-section actions-pane">
      @if (placementMode) {
        <div class="placement-info">
          <span class="placement-label">Placing: {{ placementMode.name }}</span>
          <span class="placement-hint">Click map to place, ESC to cancel</span>
          <button class="cancel-btn" (click)="cancelPlacement()">Cancel</button>
        </div>
      } @else {
        <div class="action-grid">
          @for (tower of towers; track tower.id) {
            <button
              class="action-btn tower-btn"
              [class.disabled]="!canAffordTower(tower)"
              [disabled]="!canAffordTower(tower)"
              (click)="selectTower(tower)"
              [title]="getTowerTooltip(tower)"
            >
              <span class="action-icon">{{ tower.icon }}</span>
              <span class="action-name">{{ tower.name }}</span>
              <div class="cost-row">
                <span class="cost">🪵{{ tower.cost.wood }}</span>
                <span class="cost">🪨{{ tower.cost.stone }}</span>
                <span class="cost">🪙{{ tower.cost.gold }}</span>
              </div>
            </button>
          }
          @for (building of buildings; track building.id) {
            <button
              class="action-btn"
              [class.disabled]="!canAfford(building)"
              [disabled]="!canAfford(building)"
              (click)="selectBuilding(building)"
              [title]="getBuildingTooltip(building)"
            >
              <span class="action-icon">{{ building.icon }}</span>
              <span class="action-name">{{ building.name }}</span>
              <div class="cost-row">
                <span class="cost">🪵{{ building.cost.wood }}</span>
                <span class="cost">🪨{{ building.cost.stone }}</span>
                <span class="cost">🪙{{ building.cost.gold }}</span>
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
      padding: 0.4rem;
      height: 100%;
      box-sizing: border-box;
      overflow-y: auto;
    }

    .action-grid {
      display: grid;
      grid-template-columns: repeat(8, 1fr);
      gap: 0.25rem;
    }

    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.1rem;
      padding: 0.25rem 0.2rem;
      background: #1a1a2e;
      border: 1px solid #444;
      border-radius: 4px;
      color: #fff;
      cursor: pointer;
      transition: all 0.15s;
      min-width: 0;
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
      font-size: 1.1rem;
      line-height: 1;
    }

    .action-name {
      font-size: 0.55rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100%;
    }

    .cost-row {
      display: flex;
      gap: 0.15rem;
      font-size: 0.5rem;
    }

    .cost {
      padding: 0 0.1rem;
      border-radius: 2px;
      background: rgba(0,0,0,0.3);
    }

    .placement-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      height: 100%;
      color: #aaa;
    }

    .placement-label {
      color: #00d9ff;
      font-weight: bold;
    }

    .placement-hint {
      font-size: 0.75rem;
      font-style: italic;
      color: #666;
    }

    .cancel-btn {
      padding: 0.3rem 0.6rem;
      background: #333;
      border: 1px solid #555;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.75rem;
    }

    .cancel-btn:hover {
      background: #444;
    }

    .tower-btn {
      border-color: #ff6600;
    }

    .tower-btn:hover:not(:disabled) {
      border-color: #ff9933;
      background: #2e1a1a;
    }
  `]
})
export class ActionPanelComponent {
  @Input() selectedTile: Tile | null = null;
  @Input() resources: PlayerResources = { wood: 100, stone: 100, gold: 100 };
  @Input() placementMode: BuildingDefinition | TowerDefinition | null = null;

  @Output() actionTriggered = new EventEmitter<string>();
  @Output() buildingSelected = new EventEmitter<BuildingDefinition>();
  @Output() towerSelected = new EventEmitter<TowerDefinition>();
  @Output() placementCancelled = new EventEmitter<void>();

  buildings = BUILDING_DEFINITIONS;
  towers = TOWER_DEFINITIONS;

  canAfford(building: BuildingDefinition): boolean {
    return this.resources.wood >= building.cost.wood &&
           this.resources.stone >= building.cost.stone &&
           this.resources.gold >= building.cost.gold;
  }

  canAffordTower(tower: TowerDefinition): boolean {
    return this.resources.wood >= tower.cost.wood &&
           this.resources.stone >= tower.cost.stone &&
           this.resources.gold >= tower.cost.gold;
  }

  selectBuilding(building: BuildingDefinition): void {
    if (this.canAfford(building)) {
      this.buildingSelected.emit(building);
    }
  }

  selectTower(tower: TowerDefinition): void {
    if (this.canAffordTower(tower)) {
      this.towerSelected.emit(tower);
    }
  }

  cancelPlacement(): void {
    this.placementCancelled.emit();
  }

  getBuildingTooltip(building: BuildingDefinition): string {
    const terrainNames = building.allowedTerrains.map(t => TerrainType[t]).join(', ');
    return `${building.name}\nCost: ${building.cost.wood} Wood, ${building.cost.stone} Stone, ${building.cost.gold} Gold\nCan build on: ${terrainNames}`;
  }

  getTowerTooltip(tower: TowerDefinition): string {
    const terrainNames = tower.allowedTerrains.map(t => TerrainType[t]).join(', ');
    return `${tower.name}\nDamage: ${tower.damage}, Range: ${tower.range} tiles\nCost: ${tower.cost.wood} Wood, ${tower.cost.stone} Stone, ${tower.cost.gold} Gold\nCan build on: ${terrainNames}`;
  }
}
