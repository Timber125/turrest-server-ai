import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LobbyService, SocketService, AuthService } from '../../core/services';
import { TerrainType, Tile } from '../../shared/models';
import { ChatComponent } from '../../shared/components/chat/chat.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, ChatComponent],
  template: `
    <div class="game-container">
      <header class="game-header">
        <button class="btn-back" (click)="leaveGame()">← Leave Game</button>
        <h1>{{ lobbyService.activeLobby()?.game || 'TURREST' }}</h1>
        <div class="game-controls-header">
          <span class="zoom-label">Zoom: {{ Math.round(zoom * 100) }}%</span>
          <button (click)="zoomIn()">+</button>
          <button (click)="zoomOut()">-</button>
          <button (click)="resetView()">Reset</button>
        </div>
        <div class="user-info">
          <span>{{ authService.user()?.username }}</span>
        </div>
      </header>

      <div class="game-content">
        <div class="game-main">
          <div class="game-canvas-container" #canvasContainer>
            <canvas
              #gameCanvas
              (mousedown)="onMouseDown($event)"
              (mousemove)="onMouseMove($event)"
              (mouseup)="onMouseUp($event)"
              (mouseleave)="onMouseUp($event)"
              (wheel)="onWheel($event)"
              (click)="onCanvasClick($event)"
            ></canvas>
            @if (!hasReceivedTiles) {
              <div class="game-overlay">
                <p>Game view - Waiting for map data from server...</p>
                <p class="hint">The game engine will send tile updates here.</p>
              </div>
            }
          </div>

          <div class="bottom-pane">
            <!-- Left: Selected Tile Info -->
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

            <!-- Center: Actions -->
            <div class="pane-section actions-pane">
              <h4>Actions</h4>
              <div class="action-buttons">
                <button
                  class="action-btn"
                  [disabled]="!selectedTile || !canBuildOnTile(selectedTile)"
                  (click)="onCreateTurret()"
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

            <!-- Right: Minimap -->
            <div class="pane-section minimap-pane">
              <h4>Map Overview</h4>
              <div class="minimap-container">
                <canvas
                  #minimapCanvas
                  (click)="onMinimapClick($event)"
                  (mousedown)="onMinimapMouseDown($event)"
                  (mousemove)="onMinimapMouseMove($event)"
                  (mouseup)="onMinimapMouseUp($event)"
                ></canvas>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-sidebar">
          <app-chat></app-chat>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .game-container {
      height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .game-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.5rem 1rem;
      background: #0f0f23;
      border-bottom: 1px solid #333;
      flex-shrink: 0;
    }

    .game-header h1 {
      color: #00d9ff;
      margin: 0;
      font-size: 1.2rem;
    }

    .btn-back {
      padding: 0.4rem 0.8rem;
      background: transparent;
      border: 1px solid #666;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.9rem;
    }

    .btn-back:hover {
      background: #333;
    }

    .game-controls-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .game-controls-header button {
      padding: 0.3rem 0.6rem;
      background: #333;
      border: 1px solid #555;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
    }

    .game-controls-header button:hover {
      background: #444;
    }

    .zoom-label {
      color: #aaa;
      font-size: 0.85rem;
    }

    .user-info {
      color: #fff;
      font-size: 0.9rem;
    }

    .game-content {
      display: flex;
      flex: 1;
      padding: 0.5rem;
      gap: 0.5rem;
      overflow: hidden;
      min-height: 0;
    }

    .game-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      min-width: 0;
      min-height: 0;
    }

    .game-canvas-container {
      position: relative;
      background: #0f0f23;
      border-radius: 8px;
      border: 1px solid #333;
      overflow: hidden;
      flex: 1;
      min-height: 200px;
    }

    canvas {
      display: block;
      cursor: grab;
    }

    canvas:active {
      cursor: grabbing;
    }

    .game-overlay {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      color: #666;
      pointer-events: none;
    }

    .game-overlay p {
      margin: 0.5rem 0;
    }

    .hint {
      font-size: 0.8rem;
      font-style: italic;
    }

    /* Bottom Pane */
    .bottom-pane {
      display: flex;
      gap: 0.5rem;
      height: 180px;
      flex-shrink: 0;
    }

    .pane-section {
      background: #0f0f23;
      border-radius: 8px;
      border: 1px solid #333;
      padding: 0.75rem;
      overflow: hidden;
    }

    .pane-section h4 {
      color: #00d9ff;
      margin: 0 0 0.5rem 0;
      font-size: 0.9rem;
      border-bottom: 1px solid #333;
      padding-bottom: 0.4rem;
    }

    /* Tile Info (left 20%) */
    .tile-info {
      width: 20%;
      min-width: 150px;
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

    /* Actions (center 60%) */
    .actions-pane {
      flex: 1;
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

    /* Minimap (right 20%) */
    .minimap-pane {
      width: 20%;
      min-width: 150px;
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

    .minimap-container canvas {
      width: 100%;
      height: 100%;
      cursor: pointer;
    }

    .chat-sidebar {
      width: 280px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
    }
  `]
})
export class GameComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('gameCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('canvasContainer') containerRef!: ElementRef<HTMLDivElement>;
  @ViewChild('minimapCanvas') minimapRef!: ElementRef<HTMLCanvasElement>;

  Math = Math; // Expose Math for template

  private ctx!: CanvasRenderingContext2D;
  private minimapCtx!: CanvasRenderingContext2D;
  private subscriptions: Subscription[] = [];
  private tiles: Map<string, Tile> = new Map();

  // Viewport / camera
  private cameraX = 0;
  private cameraY = 0;
  zoom = 1;
  private readonly MIN_ZOOM = 0.25;
  private readonly MAX_ZOOM = 2;
  private readonly ZOOM_STEP = 0.1;

  // Panning state
  private isDragging = false;
  private hasDragged = false;
  private lastMouseX = 0;
  private lastMouseY = 0;

  // Minimap panning
  private isMinimapDragging = false;

  // Map bounds tracking
  private mapMinX = 0;
  private mapMinY = 0;
  private mapMaxX = 0;
  private mapMaxY = 0;

  // Selection
  selectedTile: Tile | null = null;
  private selectedTileX = -1;
  private selectedTileY = -1;

  hasReceivedTiles = false;

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

  private readonly BASE_TILE_SIZE = 32;

  constructor(
    public lobbyService: LobbyService,
    public authService: AuthService,
    private socketService: SocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Listen for tile changes from server
    const tileSub = this.socketService.onCommand('GAME', 'TILE_CHANGED')
      .subscribe(cmd => {
        this.handleTileChanged(cmd.data);
      });
    this.subscriptions.push(tileSub);
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;

    // Initialize minimap after a short delay to ensure element is ready
    setTimeout(() => {
      if (this.minimapRef) {
        const minimapCanvas = this.minimapRef.nativeElement;
        this.minimapCtx = minimapCanvas.getContext('2d')!;
        this.resizeMinimap();
      }
    }, 100);

    this.resizeCanvas();
    this.render();

    // Listen for window resize
    window.addEventListener('resize', () => this.onResize());
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    window.removeEventListener('resize', () => this.onResize());
  }

  @HostListener('window:resize')
  onResize(): void {
    this.resizeCanvas();
    this.resizeMinimap();
    this.render();
  }

  private resizeCanvas(): void {
    const container = this.containerRef.nativeElement;
    const canvas = this.canvasRef.nativeElement;
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;
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

  private get tileSize(): number {
    return this.BASE_TILE_SIZE * this.zoom;
  }

  private handleTileChanged(data: Record<string, any>): void {
    const x = data['x'] as number;
    const y = data['y'] as number;
    const terrainType = data['newTerrainType'] as number;

    const tile: Tile = { x, y, terrainType };
    this.tiles.set(`${x},${y}`, tile);

    // Track map bounds
    this.mapMaxX = Math.max(this.mapMaxX, x + 1);
    this.mapMaxY = Math.max(this.mapMaxY, y + 1);

    // Update selected tile if it changed
    if (this.selectedTileX === x && this.selectedTileY === y) {
      this.selectedTile = tile;
    }

    if (!this.hasReceivedTiles) {
      this.hasReceivedTiles = true;
    }

    this.render();
  }

  private render(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = this.ctx;

    // Clear canvas
    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Calculate visible tile range for performance
    const startTileX = Math.max(0, Math.floor(this.cameraX / this.tileSize));
    const startTileY = Math.max(0, Math.floor(this.cameraY / this.tileSize));
    const endTileX = Math.ceil((this.cameraX + canvas.width) / this.tileSize);
    const endTileY = Math.ceil((this.cameraY + canvas.height) / this.tileSize);

    // Draw visible tiles
    for (let x = startTileX; x <= endTileX; x++) {
      for (let y = startTileY; y <= endTileY; y++) {
        const tile = this.tiles.get(`${x},${y}`);
        if (tile) {
          this.drawTile(tile);
        }
      }
    }

    // Draw grid overlay
    this.drawGrid();

    // Draw selection highlight
    if (this.selectedTile) {
      this.drawSelection();
    }

    // Render minimap
    this.renderMinimap();
  }

  private drawTile(tile: Tile): void {
    const screenX = tile.x * this.tileSize - this.cameraX;
    const screenY = tile.y * this.tileSize - this.cameraY;

    const color = this.getTerrainColor(tile.terrainType);
    this.ctx.fillStyle = color;
    this.ctx.fillRect(screenX, screenY, this.tileSize, this.tileSize);

    // Draw border
    this.ctx.strokeStyle = 'rgba(0, 0, 0, 0.3)';
    this.ctx.lineWidth = 1;
    this.ctx.strokeRect(screenX, screenY, this.tileSize, this.tileSize);
  }

  private drawGrid(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    this.ctx.lineWidth = 1;

    // Vertical lines
    const startX = -(this.cameraX % this.tileSize);
    for (let x = startX; x < canvas.width; x += this.tileSize) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, canvas.height);
      this.ctx.stroke();
    }

    // Horizontal lines
    const startY = -(this.cameraY % this.tileSize);
    for (let y = startY; y < canvas.height; y += this.tileSize) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(canvas.width, y);
      this.ctx.stroke();
    }
  }

  private getTerrainColor(type: TerrainType): string {
    const terrain = this.terrainTypes.find(t => t.type === type);
    return terrain?.color || '#1a1a2e';
  }

  // Mouse event handlers for panning
  onMouseDown(event: MouseEvent): void {
    this.isDragging = true;
    this.hasDragged = false;
    this.lastMouseX = event.clientX;
    this.lastMouseY = event.clientY;
  }

  onMouseMove(event: MouseEvent): void {
    if (!this.isDragging) return;

    const deltaX = event.clientX - this.lastMouseX;
    const deltaY = event.clientY - this.lastMouseY;

    // Mark as dragged if moved more than 3 pixels
    if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
      this.hasDragged = true;
    }

    this.cameraX -= deltaX;
    this.cameraY -= deltaY;

    // Clamp camera to map bounds
    this.clampCamera();

    this.lastMouseX = event.clientX;
    this.lastMouseY = event.clientY;

    this.render();
  }

  onMouseUp(event: MouseEvent): void {
    this.isDragging = false;
  }

  onWheel(event: WheelEvent): void {
    event.preventDefault();

    const oldZoom = this.zoom;
    if (event.deltaY < 0) {
      this.zoom = Math.min(this.MAX_ZOOM, this.zoom + this.ZOOM_STEP);
    } else {
      this.zoom = Math.max(this.MIN_ZOOM, this.zoom - this.ZOOM_STEP);
    }

    // Adjust camera to zoom toward mouse position
    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    const worldX = (this.cameraX + mouseX) / oldZoom;
    const worldY = (this.cameraY + mouseY) / oldZoom;

    this.cameraX = worldX * this.zoom - mouseX;
    this.cameraY = worldY * this.zoom - mouseY;

    this.clampCamera();
    this.render();
  }

  private clampCamera(): void {
    // Allow some padding beyond map bounds
    const canvas = this.canvasRef.nativeElement;
    const mapWidth = this.mapMaxX * this.tileSize;
    const mapHeight = this.mapMaxY * this.tileSize;

    const minX = -canvas.width / 2;
    const minY = -canvas.height / 2;
    const maxX = Math.max(0, mapWidth - canvas.width / 2);
    const maxY = Math.max(0, mapHeight - canvas.height / 2);

    this.cameraX = Math.max(minX, Math.min(maxX, this.cameraX));
    this.cameraY = Math.max(minY, Math.min(maxY, this.cameraY));
  }

  // Zoom controls
  zoomIn(): void {
    this.zoom = Math.min(this.MAX_ZOOM, this.zoom + this.ZOOM_STEP);
    this.clampCamera();
    this.render();
  }

  zoomOut(): void {
    this.zoom = Math.max(this.MIN_ZOOM, this.zoom - this.ZOOM_STEP);
    this.clampCamera();
    this.render();
  }

  resetView(): void {
    this.zoom = 1;
    this.cameraX = 0;
    this.cameraY = 0;
    this.render();
  }

  leaveGame(): void {
    this.lobbyService.leaveLobby();
    this.router.navigate(['/lobby']);
  }

  // Selection
  private drawSelection(): void {
    if (!this.selectedTile) return;

    const screenX = this.selectedTile.x * this.tileSize - this.cameraX;
    const screenY = this.selectedTile.y * this.tileSize - this.cameraY;

    this.ctx.strokeStyle = '#00d9ff';
    this.ctx.lineWidth = 3;
    this.ctx.strokeRect(screenX + 1, screenY + 1, this.tileSize - 2, this.tileSize - 2);

    // Draw corner markers
    const markerSize = 6;
    this.ctx.fillStyle = '#00d9ff';
    // Top-left
    this.ctx.fillRect(screenX, screenY, markerSize, 3);
    this.ctx.fillRect(screenX, screenY, 3, markerSize);
    // Top-right
    this.ctx.fillRect(screenX + this.tileSize - markerSize, screenY, markerSize, 3);
    this.ctx.fillRect(screenX + this.tileSize - 3, screenY, 3, markerSize);
    // Bottom-left
    this.ctx.fillRect(screenX, screenY + this.tileSize - 3, markerSize, 3);
    this.ctx.fillRect(screenX, screenY + this.tileSize - markerSize, 3, markerSize);
    // Bottom-right
    this.ctx.fillRect(screenX + this.tileSize - markerSize, screenY + this.tileSize - 3, markerSize, 3);
    this.ctx.fillRect(screenX + this.tileSize - 3, screenY + this.tileSize - markerSize, 3, markerSize);
  }

  onCanvasClick(event: MouseEvent): void {
    // Don't select if we were dragging
    if (this.hasDragged) return;

    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    // Convert to tile coordinates
    const tileX = Math.floor((this.cameraX + mouseX) / this.tileSize);
    const tileY = Math.floor((this.cameraY + mouseY) / this.tileSize);

    const tile = this.tiles.get(`${tileX},${tileY}`);
    if (tile) {
      this.selectedTile = tile;
      this.selectedTileX = tileX;
      this.selectedTileY = tileY;
      this.render();
    }
  }

  getTerrainName(type: TerrainType): string {
    const terrain = this.terrainTypes.find(t => t.type === type);
    return terrain?.name || 'Unknown';
  }

  canBuildOnTile(tile: Tile | null): boolean {
    if (!tile) return false;
    // Can only build on grass or dirt
    return tile.terrainType === TerrainType.GRASS || tile.terrainType === TerrainType.DIRT;
  }

  onCreateTurret(): void {
    if (!this.selectedTile || !this.canBuildOnTile(this.selectedTile)) return;
    console.log('Creating turret at', this.selectedTile.x, this.selectedTile.y);
    // TODO: Send command to server
  }

  // Minimap
  private renderMinimap(): void {
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
    const tilePixelSize = Math.max(1, this.BASE_TILE_SIZE * scale);
    this.tiles.forEach(tile => {
      const x = offsetX + tile.x * this.BASE_TILE_SIZE * scale;
      const y = offsetY + tile.y * this.BASE_TILE_SIZE * scale;
      ctx.fillStyle = this.getTerrainColor(tile.terrainType);
      ctx.fillRect(x, y, tilePixelSize, tilePixelSize);
    });

    // Draw viewport rectangle (what the main canvas is showing)
    const mainCanvas = this.canvasRef.nativeElement;
    const viewX = offsetX + (this.cameraX / this.zoom) * scale;
    const viewY = offsetY + (this.cameraY / this.zoom) * scale;
    const viewW = (mainCanvas.width / this.zoom) * scale;
    const viewH = (mainCanvas.height / this.zoom) * scale;

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
    const mainCanvas = this.canvasRef.nativeElement;
    this.cameraX = worldX * this.zoom - mainCanvas.width / 2;
    this.cameraY = worldY * this.zoom - mainCanvas.height / 2;

    this.clampCamera();
    this.render();
  }
}
