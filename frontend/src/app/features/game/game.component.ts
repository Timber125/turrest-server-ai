import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LobbyService, SocketService, AuthService } from '../../core/services';
import { TerrainType, Tile } from '../../shared/models';
import { ChatComponent } from '../../shared/components/chat/chat.component';
import { TileInfoComponent } from './components/tile-info/tile-info.component';
import { ActionPanelComponent } from './components/action-panel/action-panel.component';
import { MinimapComponent } from './components/minimap/minimap.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [
    CommonModule,
    ChatComponent,
    TileInfoComponent,
    ActionPanelComponent,
    MinimapComponent
  ],
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
            @if (countdown !== null) {
              <div class="game-overlay countdown-overlay">
                <div class="countdown-circle">
                   <span class="countdown-number">{{ countdown }}</span>
                </div>
                <p>Game starting soon...</p>
              </div>
            } @else if (!hasReceivedTiles) {
              <div class="game-overlay">
                <p>Game view - Waiting for map data from server...</p>
                <p class="hint">The game engine will send tile updates here.</p>
              </div>
            }
          </div>

          <div class="bottom-pane">
            <!-- Left: Selected Tile Info -->
            <app-tile-info
               class="pane-section-wrapper tile-info" 
               [selectedTile]="selectedTile">
            </app-tile-info>

            <!-- Center: Actions -->
            <app-action-panel
               class="pane-section-wrapper actions-pane"
               [selectedTile]="selectedTile"
               (actionTriggered)="onActionTriggered($event)">
            </app-action-panel>

            <!-- Right: Minimap -->
            <app-minimap
               class="pane-section-wrapper minimap-pane"
               [tiles]="tiles"
               [mapMaxX]="mapMaxX"
               [mapMaxY]="mapMaxY"
               [cameraX]="cameraX"
               [cameraY]="cameraY"
               [zoom]="zoom"
               [selectedTile]="selectedTile"
               [mainCanvasWidth]="mainCanvasWidth"
               [mainCanvasHeight]="mainCanvasHeight"
               (cameraMove)="onCameraMove($event)">
            </app-minimap>
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

    /* Tile Info (left 20%) */
    .tile-info {
      width: 20%;
      min-width: 150px;
    }

    /* Actions (center 60%) */
    .actions-pane {
      flex: 1;
    }

    /* Minimap (right 20%) */
    .minimap-pane {
      width: 20%;
      min-width: 150px;
    }

    /* Wrapper helper to ensure components take full height */
    .pane-section-wrapper {
        display: block;
        height: 100%;
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

  Math = Math; // Expose Math for template

  private ctx!: CanvasRenderingContext2D;
  private subscriptions: Subscription[] = [];
  tiles: Map<string, Tile> = new Map();

  // Viewport / camera
  cameraX = 0;
  cameraY = 0;
  zoom = 1;
  private readonly MIN_ZOOM = 0.25;
  private readonly MAX_ZOOM = 2;
  private readonly ZOOM_STEP = 0.1;

  // Panning state
  private isDragging = false;
  private hasDragged = false;
  private lastMouseX = 0;
  private lastMouseY = 0;

  // Map bounds tracking
  mapMaxX = 0;
  mapMaxY = 0;

  // Canvas dimensions for minimap
  mainCanvasWidth = 0;
  mainCanvasHeight = 0;

  // Selection
  selectedTile: Tile | null = null;
  private selectedTileX = -1;
  private selectedTileY = -1;

  hasReceivedTiles = false;
  countdown: number | null = null;

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
  ) { }

  ngOnInit(): void {
    // Listen for tile changes from server
    const tileSub = this.socketService.onCommand('GAME', 'TILE_CHANGED')
      .subscribe(cmd => {
        this.hasReceivedTiles = true;
        this.handleTileChanged(cmd.data);
      });
    this.subscriptions.push(tileSub);

    // Listen for countdown
    const countdownSub = this.socketService.onCommand('GAME', 'COUNTDOWN')
      .subscribe(cmd => {
        this.countdown = cmd.data['seconds'];
        this.hasReceivedTiles = false; // Reset to show countdown
        this.startLocalCountdown();
      });
    this.subscriptions.push(countdownSub);
  }

  private startLocalCountdown(): void {
    const timer = setInterval(() => {
      if (this.countdown !== null && this.countdown > 0) {
        this.countdown--;
      } else {
        this.countdown = null;
        clearInterval(timer);
      }
    }, 1000);
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;

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
    this.render();
  }

  private resizeCanvas(): void {
    const container = this.containerRef.nativeElement;
    const canvas = this.canvasRef.nativeElement;
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;

    // Update public properties for minimap
    this.mainCanvasWidth = canvas.width;
    this.mainCanvasHeight = canvas.height;
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
    this.lobbyService.leaveGame();
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

  // Action Panel Handler
  onActionTriggered(action: string): void {
    console.log('Action triggered:', action);
    if (action === 'createTurret') {
      // TODO: Implement create turret logic
      if (this.selectedTile) {
        console.log('Creating turret at', this.selectedTile.x, this.selectedTile.y);
      }
    }
  }

  // Minimap Navigation Handler
  onCameraMove(pos: { x: number, y: number }): void {
    this.cameraX = pos.x;
    this.cameraY = pos.y;
    this.clampCamera();
    this.render();
  }
}
