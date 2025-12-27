import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Tile, TerrainType } from '../../../../shared/models';

@Component({
    selector: 'app-minimap',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="pane-section minimap-pane">
      <h4>Map Overview</h4>
      <div class="minimap-container">
        <canvas
          #minimapCanvas
          (click)="onMinimapClick($event)"
          (mousedown)="onMinimapMouseDown($event)"
          (mousemove)="onMinimapMouseMove($event)"
          (mouseup)="onMinimapMouseUp($event)"
          (mouseleave)="onMinimapMouseUp($event)"
        ></canvas>
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

    .minimap-container {
      position: relative;
      width: 100%;
      height: calc(100% - 30px);
      background: #000;
      border: 1px solid #444;
      border-radius: 4px;
      overflow: hidden;
    }

    canvas {
      width: 100%;
      height: 100%;
      cursor: pointer;
    }
  `]
})
export class MinimapComponent implements AfterViewInit, OnChanges, OnDestroy {
    @Input() tiles: Map<string, Tile> = new Map();
    @Input() mapMaxX = 0;
    @Input() mapMaxY = 0;
    @Input() cameraX = 0;
    @Input() cameraY = 0;
    @Input() zoom = 1;
    @Input() selectedTile: Tile | null = null;
    @Input() mainCanvasWidth = 0;
    @Input() mainCanvasHeight = 0;

    @Output() cameraMove = new EventEmitter<{ x: number, y: number }>();

    @ViewChild('minimapCanvas') minimapRef!: ElementRef<HTMLCanvasElement>;

    private minimapCtx!: CanvasRenderingContext2D;
    private readonly BASE_TILE_SIZE = 32;
    private isMinimapDragging = false;
    private resizeObserver: ResizeObserver | null = null;

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

    ngAfterViewInit(): void {
        if (this.minimapRef) {
            const minimapCanvas = this.minimapRef.nativeElement;
            this.minimapCtx = minimapCanvas.getContext('2d')!;

            // Observe size changes
            this.resizeObserver = new ResizeObserver(() => {
                this.resizeMinimap();
                this.renderMinimap();
            });
            this.resizeObserver.observe(minimapCanvas.parentElement!);

            this.resizeMinimap();
            this.renderMinimap();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.minimapCtx) {
            this.renderMinimap();
        }
    }

    ngOnDestroy(): void {
        if (this.resizeObserver) {
            this.resizeObserver.disconnect();
        }
    }

    private resizeMinimap(): void {
        if (!this.minimapRef) return;
        const minimapCanvas = this.minimapRef.nativeElement;
        const container = minimapCanvas.parentElement;
        if (container) {
            minimapCanvas.width = container.clientWidth;
            minimapCanvas.height = container.clientHeight;
        }
    }

    getTerrainColor(type: TerrainType): string {
        const terrain = this.terrainTypes.find(t => t.type === type);
        return terrain?.color || '#1a1a2e';
    }

    renderMinimap(): void {
        if (!this.minimapCtx || !this.minimapRef) return;

        const canvas = this.minimapRef.nativeElement;
        const ctx = this.minimapCtx;

        // Clear minimap
        ctx.fillStyle = '#0a0a15';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        if (this.mapMaxX === 0 || this.mapMaxY === 0) return;

        // Calculate scale to fit map in minimap
        const scaleX = canvas.width / (this.mapMaxX * this.BASE_TILE_SIZE);
        const scaleY = canvas.height / (this.mapMaxY * this.BASE_TILE_SIZE);
        const scale = Math.min(scaleX, scaleY);

        // Offset to center the map
        const mapPixelWidth = this.mapMaxX * this.BASE_TILE_SIZE * scale;
        const mapPixelHeight = this.mapMaxY * this.BASE_TILE_SIZE * scale;
        const offsetX = (canvas.width - mapPixelWidth) / 2;
        const offsetY = (canvas.height - mapPixelHeight) / 2;

        // Draw all tiles
        // Optimization: Draw to an offscreen canvas if this becomes too heavy
        const tilePixelSize = Math.max(1, this.BASE_TILE_SIZE * scale);
        this.tiles.forEach(tile => {
            const x = offsetX + tile.x * this.BASE_TILE_SIZE * scale;
            const y = offsetY + tile.y * this.BASE_TILE_SIZE * scale;
            ctx.fillStyle = this.getTerrainColor(tile.terrainType);
            ctx.fillRect(x, y, tilePixelSize, tilePixelSize);
        });

        // Draw viewport rectangle (what the main canvas is showing)
        const viewX = offsetX + (this.cameraX / this.zoom) * scale;
        const viewY = offsetY + (this.cameraY / this.zoom) * scale;
        const viewW = (this.mainCanvasWidth / this.zoom) * scale;
        const viewH = (this.mainCanvasHeight / this.zoom) * scale;

        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 2;
        ctx.strokeRect(viewX, viewY, viewW, viewH);

        // Draw selected tile marker
        if (this.selectedTile) {
            const selX = offsetX + this.selectedTile.x * this.BASE_TILE_SIZE * scale;
            const selY = offsetY + this.selectedTile.y * this.BASE_TILE_SIZE * scale;
            ctx.strokeStyle = '#00d9ff';
            ctx.lineWidth = 2;
            ctx.strokeRect(selX - 1, selY - 1, tilePixelSize + 2, tilePixelSize + 2);
        }
    }

    onMinimapClick(event: MouseEvent): void {
        this.navigateFromMinimap(event);
    }

    onMinimapMouseDown(event: MouseEvent): void {
        this.isMinimapDragging = true;
        this.navigateFromMinimap(event);
    }

    onMinimapMouseMove(event: MouseEvent): void {
        if (!this.isMinimapDragging) return;
        this.navigateFromMinimap(event);
    }

    onMinimapMouseUp(event: MouseEvent): void {
        this.isMinimapDragging = false;
    }

    private navigateFromMinimap(event: MouseEvent): void {
        if (!this.minimapRef || this.mapMaxX === 0 || this.mapMaxY === 0) return;

        const canvas = this.minimapRef.nativeElement;
        const rect = canvas.getBoundingClientRect();
        const mouseX = event.clientX - rect.left;
        const mouseY = event.clientY - rect.top;

        // Calculate scale
        const scaleX = canvas.width / (this.mapMaxX * this.BASE_TILE_SIZE);
        const scaleY = canvas.height / (this.mapMaxY * this.BASE_TILE_SIZE);
        const scale = Math.min(scaleX, scaleY);

        // Offset
        const mapPixelWidth = this.mapMaxX * this.BASE_TILE_SIZE * scale;
        const mapPixelHeight = this.mapMaxY * this.BASE_TILE_SIZE * scale;
        const offsetX = (canvas.width - mapPixelWidth) / 2;
        const offsetY = (canvas.height - mapPixelHeight) / 2;

        // Convert click to world position
        const worldX = (mouseX - offsetX) / scale;
        const worldY = (mouseY - offsetY) / scale;

        // Center camera on this position
        const newCameraX = worldX * this.zoom - this.mainCanvasWidth / 2;
        const newCameraY = worldY * this.zoom - this.mainCanvasHeight / 2;

        this.cameraMove.emit({ x: newCameraX, y: newCameraY });
    }
}
