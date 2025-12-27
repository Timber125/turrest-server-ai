import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Tile, TerrainType } from '../../../../shared/models';

@Component({
    selector: 'app-tile-info',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="pane-section tile-info">
      <h4>Selected Tile</h4>
      @if (selectedTile) {
        <div class="tile-details">
          <p><span class="label">Position:</span> ({{ selectedTile.x }}, {{ selectedTile.y }})</p>
          <p><span class="label">Terrain:</span> {{ getTerrainName(selectedTile.terrainType) }}</p>
          <div class="terrain-preview" [style.background-color]="getTerrainColor(selectedTile.terrainType)"></div>
        </div>
      } @else {
        <p class="no-selection">Click a tile to select</p>
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

    .tile-details p {
      color: #fff;
      margin: 0.3rem 0;
      font-size: 0.85rem;
    }

    .tile-details .label {
      color: #888;
    }

    .terrain-preview {
      width: 40px;
      height: 40px;
      border-radius: 4px;
      border: 2px solid #555;
      margin-top: 0.5rem;
    }

    .no-selection {
      color: #666;
      font-style: italic;
      font-size: 0.85rem;
    }
  `]
})
export class TileInfoComponent {
    @Input() selectedTile: Tile | null = null;

    terrainTypes = [
        { type: TerrainType.GRASS, name: 'Grass', color: '#4CAF50' },
        { type: TerrainType.DIRT, name: 'Dirt', color: '#8B4513' },
        { type: TerrainType.FOREST, name: 'Forest', color: '#228B22' },
        { type: TerrainType.WATER_SHALLOW, name: 'Shallow Water', color: '#87CEEB' },
        { type: TerrainType.WATER_DEEP, name: 'Deep Water', color: '#1E90FF' },
        { type: TerrainType.ROCKY, name: 'Rocky', color: '#808080' },
        { type: TerrainType.CASTLE, name: 'Castle', color: '#FFD700' },
        { type: TerrainType.SPAWNER, name: 'Spawner', color: '#FF4500' }
    ];

    getTerrainName(type: TerrainType): string {
        const terrain = this.terrainTypes.find(t => t.type === type);
        return terrain?.name || 'Unknown';
    }

    getTerrainColor(type: TerrainType): string {
        const terrain = this.terrainTypes.find(t => t.type === type);
        return terrain?.color || '#1a1a2e';
    }
}
