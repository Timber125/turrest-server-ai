import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, HostListener, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LobbyService, SocketService, AuthService } from '../../core/services';
import { TerrainType, Tile, StructureType, PlayerResources, BuildingDefinition, BUILDING_DEFINITIONS, Creep } from '../../shared/models';
import { getPlayerColor } from '../../shared/constants/player-colors';
import { ChatComponent } from '../../shared/components/chat/chat.component';
import { TileInfoComponent } from './components/tile-info/tile-info.component';
import { ActionPanelComponent } from './components/action-panel/action-panel.component';
import { MinimapComponent } from './components/minimap/minimap.component';
import { ResourceBarComponent } from './components/resource-bar/resource-bar.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [
    CommonModule,
    ChatComponent,
    TileInfoComponent,
    ActionPanelComponent,
    MinimapComponent,
    ResourceBarComponent
  ],
  template: `
    <div class="game-container">
      <header class="game-header">
        <button class="btn-back" (click)="leaveGame()">← Leave Game</button>
        <app-resource-bar [resources]="resources"></app-resource-bar>
        <h1>{{ lobbyService.activeLobby()?.game || 'TURREST' }}</h1>
        <div class="game-controls-header">
          <span class="zoom-label">Zoom: {{ Math.round(zoom * 100) }}%</span>
          <button (click)="zoomIn()">+</button>
          <button (click)="zoomOut()">-</button>
          <button (click)="resetView()">Reset</button>
        </div>
        <div class="user-info">
          <div class="hp-display" [class.low-hp]="myHitpoints <= 5">
            <span class="hp-icon">HP</span>
            <span class="hp-value">{{ myHitpoints }}</span>
          </div>
          <div class="player-color-indicator" [style.background-color]="getMyPlayerColor()">
            <span class="player-number">P{{ myPlayerNumber }}</span>
          </div>
          <span>{{ authService.user()?.username }}</span>
        </div>
      </header>

      <div class="game-content">
        <div class="game-main">
          <div class="game-canvas-container" #canvasContainer>
            <canvas
              #gameCanvas
              [class.placement-mode]="placementMode !== null"
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
            @if (errorMessage) {
              <div class="error-toast">{{ errorMessage }}</div>
            }
            @if (gameOver) {
              <div class="game-overlay game-over-overlay">
                <h2>{{ gameOverMessage }}</h2>
                <button class="btn-back-to-lobby" (click)="leaveGame()">Back to Lobby</button>
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
               [resources]="resources"
               [placementMode]="placementMode"
               (actionTriggered)="onActionTriggered($event)"
               (buildingSelected)="onBuildingSelected($event)"
               (placementCancelled)="cancelPlacementMode()">
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
      gap: 1rem;
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
      display: flex;
      align-items: center;
      gap: 0.75rem;
      color: #fff;
      font-size: 0.9rem;
    }

    .hp-display {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      background: #333;
      padding: 0.4rem 0.6rem;
      border-radius: 6px;
      border: 2px solid #4CAF50;
    }

    .hp-display.low-hp {
      border-color: #F44336;
      animation: pulse-red 1s infinite;
    }

    @keyframes pulse-red {
      0%, 100% { background: #333; }
      50% { background: rgba(244, 67, 54, 0.3); }
    }

    .hp-icon {
      color: #4CAF50;
      font-weight: bold;
      font-size: 0.7rem;
    }

    .hp-display.low-hp .hp-icon {
      color: #F44336;
    }

    .hp-value {
      font-weight: bold;
      font-size: 1rem;
    }

    .player-color-indicator {
      width: 36px;
      height: 36px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 3px solid #fff;
      box-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
    }

    .player-number {
      font-weight: bold;
      font-size: 0.85rem;
      color: #fff;
      text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
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

    canvas.placement-mode {
      cursor: crosshair;
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

    .game-over-overlay {
      background: rgba(0, 0, 0, 0.85);
      padding: 2rem;
      border-radius: 12px;
      border: 2px solid #00d9ff;
      pointer-events: all;
    }

    .game-over-overlay h2 {
      color: #00d9ff;
      font-size: 1.8rem;
      margin-bottom: 1.5rem;
    }

    .btn-back-to-lobby {
      padding: 0.75rem 1.5rem;
      background: #00d9ff;
      border: none;
      color: #000;
      font-weight: bold;
      border-radius: 6px;
      cursor: pointer;
      font-size: 1rem;
    }

    .btn-back-to-lobby:hover {
      background: #00b8d9;
    }

    .error-toast {
      position: absolute;
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(255, 0, 0, 0.8);
      color: #fff;
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      font-weight: bold;
      animation: fadeInOut 3s ease-in-out;
    }

    @keyframes fadeInOut {
      0% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
      10% { opacity: 1; transform: translateX(-50%) translateY(0); }
      80% { opacity: 1; }
      100% { opacity: 0; }
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

  // Resources
  resources: PlayerResources = { wood: 100, stone: 100, gold: 100 };

  // Player info
  myPlayerNumber = 0;  // Set from server on game start
  myColorIndex = 0;    // Color index from lobby selection
  myHitpoints = 20;    // Player hitpoints
  playerColorMap: Map<number, number> = new Map(); // playerNumber -> colorIndex

  // Creeps
  creeps: Map<string, Creep> = new Map();
  gameOver = false;
  gameOverMessage = '';
  private lastFrameTime = 0;
  private animationFrameId: number | null = null;

  // Placement mode
  placementMode: BuildingDefinition | null = null;
  hoverTileX = -1;
  hoverTileY = -1;

  // Error message
  errorMessage: string | null = null;

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

  // Road autotiling images (16 variations based on NESW connections)
  private roadImages: Map<string, HTMLImageElement> = new Map();
  private roadImagesLoaded = false;

  // Creep sprite images
  private creepSprites: Map<string, HTMLImageElement> = new Map();

  constructor(
    public lobbyService: LobbyService,
    public authService: AuthService,
    private socketService: SocketService,
    private router: Router,
    private ngZone: NgZone
  ) { }

  ngOnInit(): void {
    // Listen for tile updates (new format with structure data)
    const tileUpdateSub = this.socketService.onCommand('GAME', 'TILE_UPDATE')
      .subscribe(cmd => {
        this.hasReceivedTiles = true;
        this.handleTileUpdate(cmd.data);
      });
    this.subscriptions.push(tileUpdateSub);

    // Legacy: Listen for tile changes (old format)
    const tileSub = this.socketService.onCommand('GAME', 'TILE_CHANGED')
      .subscribe(cmd => {
        this.hasReceivedTiles = true;
        this.handleTileChanged(cmd.data);
      });
    this.subscriptions.push(tileSub);

    // Listen for resource updates - run in NgZone to trigger change detection
    const resourceSub = this.socketService.onCommand('GAME', 'RESOURCE_UPDATE')
      .subscribe(cmd => {
        this.ngZone.run(() => this.handleResourceUpdate(cmd.data));
      });
    this.subscriptions.push(resourceSub);

    // Listen for building changes
    const buildingSub = this.socketService.onCommand('GAME', 'BUILDING_CHANGED')
      .subscribe(cmd => {
        this.handleBuildingChanged(cmd.data);
      });
    this.subscriptions.push(buildingSub);

    // Listen for error messages
    const errorSub = this.socketService.onCommand('GAME', 'ERROR_MESSAGE')
      .subscribe(cmd => {
        this.showError(cmd.data['message'] as string);
      });
    this.subscriptions.push(errorSub);

    // Listen for countdown
    const countdownSub = this.socketService.onCommand('GAME', 'COUNTDOWN')
      .subscribe(cmd => {
        this.countdown = cmd.data['seconds'];
        this.hasReceivedTiles = false;
        this.startLocalCountdown();
      });
    this.subscriptions.push(countdownSub);

    // Listen for player info (tells us our player number and color)
    const playerInfoSub = this.socketService.onCommand('GAME', 'PLAYER_INFO')
      .subscribe(cmd => {
        this.myPlayerNumber = cmd.data['playerNumber'] as number;
        this.myColorIndex = cmd.data['colorIndex'] as number;
        console.log('Received player info - player number:', this.myPlayerNumber, 'colorIndex:', this.myColorIndex);
      });
    this.subscriptions.push(playerInfoSub);

    // Listen for full map (bulk load - much faster than tile-by-tile)
    const fullMapSub = this.socketService.onCommand('GAME', 'FULL_MAP')
      .subscribe(cmd => {
        this.handleFullMap(cmd.data);
      });
    this.subscriptions.push(fullMapSub);

    // Listen for creep spawn
    const spawnCreepSub = this.socketService.onCommand('GAME', 'SPAWN_CREEP')
      .subscribe(cmd => {
        this.handleSpawnCreep(cmd.data);
      });
    this.subscriptions.push(spawnCreepSub);

    // Listen for creep update
    const updateCreepSub = this.socketService.onCommand('GAME', 'UPDATE_CREEP')
      .subscribe(cmd => {
        this.handleUpdateCreep(cmd.data);
      });
    this.subscriptions.push(updateCreepSub);

    // Listen for creep despawn
    const despawnCreepSub = this.socketService.onCommand('GAME', 'DESPAWN_CREEP')
      .subscribe(cmd => {
        this.handleDespawnCreep(cmd.data);
      });
    this.subscriptions.push(despawnCreepSub);

    // Listen for player damage - run in NgZone to trigger change detection
    const damageSub = this.socketService.onCommand('GAME', 'PLAYER_TAKES_DAMAGE')
      .subscribe(cmd => {
        this.ngZone.run(() => this.handlePlayerDamage(cmd.data));
      });
    this.subscriptions.push(damageSub);

    // Listen for game over - run in NgZone to trigger change detection
    const gameOverSub = this.socketService.onCommand('GAME', 'GAME_OVER')
      .subscribe(cmd => {
        this.ngZone.run(() => this.handleGameOver(cmd.data));
      });
    this.subscriptions.push(gameOverSub);
  }

  private showError(message: string): void {
    this.errorMessage = message;
    setTimeout(() => {
      this.errorMessage = null;
    }, 3000);
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
    this.loadRoadImages();
    this.loadCreepSprites();
    this.startAnimationLoop();

    window.addEventListener('resize', () => this.onResize());
  }

  private startAnimationLoop(): void {
    const animate = (timestamp: number) => {
      const deltaTime = this.lastFrameTime > 0
        ? (timestamp - this.lastFrameTime) / 1000
        : 0;
      this.lastFrameTime = timestamp;

      this.updateCreepPositions(deltaTime);
      this.render();

      this.animationFrameId = requestAnimationFrame(animate);
    };
    this.animationFrameId = requestAnimationFrame(animate);
  }

  private updateCreepPositions(deltaTime: number): void {
    if (deltaTime <= 0) return;

    for (const creep of this.creeps.values()) {
      const dx = creep.targetX - creep.x;
      const dy = creep.targetY - creep.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance > 0.01) {
        const moveDistance = creep.speed * deltaTime;
        const ratio = Math.min(moveDistance / distance, 1);
        creep.x += dx * ratio;
        creep.y += dy * ratio;
      } else {
        // Snap to target when very close
        creep.x = creep.targetX;
        creep.y = creep.targetY;
      }
    }
  }

  private loadRoadImages(): void {
    const variations = [
      '0000', '1000', '0100', '0010', '0001',
      '1100', '1010', '0101', '1001', '0110', '0011',
      '1110', '1101', '1011', '0111', '1111'
    ];

    let loadedCount = 0;
    for (const v of variations) {
      const img = new Image();
      img.onload = () => {
        loadedCount++;
        if (loadedCount === variations.length) {
          this.roadImagesLoaded = true;
          this.render();
        }
      };
      img.onerror = () => {
        loadedCount++;
        console.warn(`Failed to load road image: road_${v}.png`);
      };
      img.src = `/assets/tiles/roads/road_${v}.png`;
      this.roadImages.set(v, img);
    }
  }

  private loadCreepSprites(): void {
    const creepTypes = ['GHOST', 'TROLL'];
    for (const type of creepTypes) {
      const img = new Image();
      img.onload = () => {
        console.log(`Loaded creep sprite: ${type} (${img.naturalWidth}x${img.naturalHeight})`);
      };
      img.onerror = () => {
        console.error(`Failed to load creep sprite: /assets/sprites/creeps/${type.toLowerCase()}.png`);
      };
      img.src = `/assets/sprites/creeps/${type.toLowerCase()}.png`;
      this.creepSprites.set(type, img);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    window.removeEventListener('resize', () => this.onResize());
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    this.resizeCanvas();
    this.render();
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.placementMode) {
      this.cancelPlacementMode();
    }
  }

  private resizeCanvas(): void {
    const container = this.containerRef.nativeElement;
    const canvas = this.canvasRef.nativeElement;
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;

    this.mainCanvasWidth = canvas.width;
    this.mainCanvasHeight = canvas.height;
  }

  private get tileSize(): number {
    return this.BASE_TILE_SIZE * this.zoom;
  }

  private handleTileUpdate(data: Record<string, any>): void {
    const x = data['x'] as number;
    const y = data['y'] as number;
    const terrainType = data['terrainType'] as number;
    const structureType = data['structureType'] as number | undefined;
    const buildingType = data['buildingType'] as number | undefined;
    const ownerPlayerNumber = data['ownerPlayerNumber'] as number | undefined;
    const owners = data['owners'] as number[] | undefined;

    const tile: Tile = {
      x,
      y,
      terrainType,
      structureType: structureType !== undefined ? structureType : undefined,
      buildingType,
      ownerPlayerNumber,
      owners: owners || []
    };
    this.tiles.set(`${x},${y}`, tile);

    this.mapMaxX = Math.max(this.mapMaxX, x + 1);
    this.mapMaxY = Math.max(this.mapMaxY, y + 1);

    if (this.selectedTileX === x && this.selectedTileY === y) {
      this.selectedTile = tile;
    }

    if (!this.hasReceivedTiles) {
      this.hasReceivedTiles = true;
    }

    this.render();
  }

  private handleTileChanged(data: Record<string, any>): void {
    const x = data['x'] as number;
    const y = data['y'] as number;
    const terrainType = data['newTerrainType'] as number;

    const existingTile = this.tiles.get(`${x},${y}`);
    const tile: Tile = {
      x,
      y,
      terrainType,
      structureType: existingTile?.structureType,
      buildingType: existingTile?.buildingType,
      ownerPlayerNumber: existingTile?.ownerPlayerNumber
    };
    this.tiles.set(`${x},${y}`, tile);

    this.mapMaxX = Math.max(this.mapMaxX, x + 1);
    this.mapMaxY = Math.max(this.mapMaxY, y + 1);

    if (this.selectedTileX === x && this.selectedTileY === y) {
      this.selectedTile = tile;
    }

    if (!this.hasReceivedTiles) {
      this.hasReceivedTiles = true;
    }

    this.render();
  }

  private handleResourceUpdate(data: Record<string, any>): void {
    const wood = data['wood'] as number;
    const stone = data['stone'] as number;
    const gold = data['gold'] as number;

    // Only update if values actually changed (avoid unnecessary change detection)
    if (this.resources.wood !== wood ||
        this.resources.stone !== stone ||
        this.resources.gold !== gold) {
      this.resources = { wood, stone, gold };
    }
  }

  private handleBuildingChanged(data: Record<string, any>): void {
    const x = data['x'] as number;
    const y = data['y'] as number;
    const buildingType = data['buildingType'] as number;
    const playerNumber = data['playerNumber'] as number;

    const tile = this.tiles.get(`${x},${y}`);
    if (tile) {
      tile.structureType = StructureType.BUILDING;
      tile.buildingType = buildingType;
      tile.ownerPlayerNumber = playerNumber;
      this.render();
    }
  }

  private handleFullMap(data: Record<string, any>): void {
    const width = data['width'] as number;
    const height = data['height'] as number;
    const tilesData = data['tiles'] as Array<Record<string, any>>;
    const colorMap = data['playerColorMap'] as Record<string, number> | undefined;

    console.log(`Received full map: ${width}x${height} with ${tilesData.length} tiles`);

    // Store player color mapping (playerNumber -> colorIndex)
    if (colorMap) {
      this.playerColorMap.clear();
      for (const [playerNum, colorIdx] of Object.entries(colorMap)) {
        this.playerColorMap.set(parseInt(playerNum), colorIdx);
      }
      console.log('Player color map:', this.playerColorMap);
    }

    this.mapMaxX = width;
    this.mapMaxY = height;
    this.tiles.clear();

    for (const tileData of tilesData) {
      const tile: Tile = {
        x: tileData['x'] as number,
        y: tileData['y'] as number,
        terrainType: tileData['terrainType'] as number,
        structureType: tileData['structureType'] as number | undefined,
        buildingType: tileData['buildingType'] as number | undefined,
        ownerPlayerNumber: tileData['ownerPlayerNumber'] as number | undefined,
        owners: tileData['owners'] as number[] || []
      };
      this.tiles.set(`${tile.x},${tile.y}`, tile);
    }

    this.hasReceivedTiles = true;
    this.render();
  }

  private handleSpawnCreep(data: Record<string, any>): void {
    const x = data['x'] as number;
    const y = data['y'] as number;
    const spawnedByPlayer = data['spawnedByPlayer'] as number | null ?? null;
    const creep: Creep = {
      id: data['creepId'] as string,
      creepType: data['creepType'] as string,
      x: x,
      y: y,
      targetX: x,
      targetY: y,
      playerNumber: data['playerNumber'] as number,
      spawnedByPlayer: spawnedByPlayer,
      hitpoints: data['hitpoints'] as number,
      maxHitpoints: data['maxHitpoints'] as number,
      speed: data['speed'] as number || 0.33
    };
    this.creeps.set(creep.id, creep);
    console.log('Spawned creep:', creep.id, 'at', creep.x, creep.y, 'spawnedBy:', spawnedByPlayer);
  }

  private handleUpdateCreep(data: Record<string, any>): void {
    const creepId = data['creepId'] as string;
    const creep = this.creeps.get(creepId);
    if (creep) {
      // Set target position for interpolation (don't jump immediately)
      creep.targetX = data['x'] as number;
      creep.targetY = data['y'] as number;
      creep.hitpoints = data['hitpoints'] as number;
    }
  }

  private handleDespawnCreep(data: Record<string, any>): void {
    const creepId = data['creepId'] as string;
    this.creeps.delete(creepId);
    console.log('Despawned creep:', creepId);
    this.render();
  }

  private handlePlayerDamage(data: Record<string, any>): void {
    const playerNumber = data['playerNumber'] as number;
    const damage = data['damage'] as number;
    const remainingHitpoints = data['remainingHitpoints'] as number;

    if (playerNumber === this.myPlayerNumber) {
      this.myHitpoints = remainingHitpoints;
      console.log(`Took ${damage} damage! HP: ${remainingHitpoints}`);
    }
  }

  private handleGameOver(data: Record<string, any>): void {
    const playerNumber = data['playerNumber'] as number;
    const isWinner = data['isWinner'] as boolean;

    console.log('Game over event received:', { playerNumber, isWinner, myPlayerNumber: this.myPlayerNumber });

    if (isWinner) {
      // Winner announcement - show to everyone who hasn't already seen game over
      if (playerNumber === this.myPlayerNumber) {
        this.gameOver = true;
        this.gameOverMessage = 'Victory! You are the last player standing!';
      } else if (!this.gameOver) {
        // Only show "Player X wins" if we haven't already lost
        this.gameOver = true;
        this.gameOverMessage = `Player ${playerNumber + 1} wins!`;
      }
    } else if (playerNumber === this.myPlayerNumber) {
      // I lost
      this.gameOver = true;
      this.gameOverMessage = 'Game Over - Your castle has fallen!';
    }
  }

  private render(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = this.ctx;

    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    const startTileX = Math.max(0, Math.floor(this.cameraX / this.tileSize));
    const startTileY = Math.max(0, Math.floor(this.cameraY / this.tileSize));
    const endTileX = Math.ceil((this.cameraX + canvas.width) / this.tileSize);
    const endTileY = Math.ceil((this.cameraY + canvas.height) / this.tileSize);

    for (let x = startTileX; x <= endTileX; x++) {
      for (let y = startTileY; y <= endTileY; y++) {
        const tile = this.tiles.get(`${x},${y}`);
        if (tile) {
          this.drawTile(tile);
        }
      }
    }

    this.drawGrid();

    // Draw creeps
    this.drawCreeps();

    if (this.selectedTile) {
      this.drawSelection();
    }

    // Draw placement preview
    if (this.placementMode && this.hoverTileX >= 0 && this.hoverTileY >= 0) {
      this.drawPlacementPreview();
    }
  }

  private drawCreeps(): void {
    for (const creep of this.creeps.values()) {
      // creep.x and creep.y already include +0.5 offset from backend (centered on tile)
      const centerX = creep.x * this.tileSize - this.cameraX;
      const centerY = creep.y * this.tileSize - this.cameraY;

      // Skip if off-screen
      const canvas = this.canvasRef.nativeElement;
      if (centerX < -this.tileSize || centerX > canvas.width + this.tileSize ||
          centerY < -this.tileSize || centerY > canvas.height + this.tileSize) {
        continue;
      }

      const size = this.tileSize * 0.6;

      // Try to draw sprite
      const sprite = this.creepSprites.get(creep.creepType);
      if (sprite && sprite.complete && sprite.naturalWidth > 0) {
        // Draw sprite centered on position
        this.ctx.drawImage(sprite, centerX - size / 2, centerY - size / 2, size, size);
      } else {
        // Fallback: Draw creep body (circle)
        this.ctx.beginPath();
        this.ctx.arc(centerX, centerY, size / 2, 0, Math.PI * 2);
        this.ctx.fillStyle = creep.creepType === 'TROLL' ? '#5a4a3a' : '#9090c0';
        this.ctx.fill();
      }

      // Draw contour ring (only if sent by a player, not wave-spawned)
      if (creep.spawnedByPlayer !== null && creep.spawnedByPlayer !== undefined) {
        const senderColorIndex = this.playerColorMap.get(creep.spawnedByPlayer) ?? creep.spawnedByPlayer;
        this.ctx.beginPath();
        this.ctx.arc(centerX, centerY, size / 2 + 2, 0, Math.PI * 2);
        this.ctx.strokeStyle = getPlayerColor(senderColorIndex);
        this.ctx.lineWidth = 2;
        this.ctx.stroke();
      }

      // Draw health bar
      const hpBarWidth = size;
      const hpBarHeight = 4;
      const hpBarX = centerX - hpBarWidth / 2;
      const hpBarY = centerY - size / 2 - hpBarHeight - 2;
      const hpPercent = creep.hitpoints / creep.maxHitpoints;

      // Background
      this.ctx.fillStyle = '#333';
      this.ctx.fillRect(hpBarX, hpBarY, hpBarWidth, hpBarHeight);

      // Health
      const hpColor = hpPercent > 0.5 ? '#4CAF50' : hpPercent > 0.25 ? '#FFC107' : '#F44336';
      this.ctx.fillStyle = hpColor;
      this.ctx.fillRect(hpBarX, hpBarY, hpBarWidth * hpPercent, hpBarHeight);
    }
  }

  private drawTile(tile: Tile): void {
    const screenX = tile.x * this.tileSize - this.cameraX;
    const screenY = tile.y * this.tileSize - this.cameraY;

    // Draw terrain
    const color = this.getTerrainColor(tile.terrainType);
    this.ctx.fillStyle = color;
    this.ctx.fillRect(screenX, screenY, this.tileSize, this.tileSize);

    // Draw road overlay with autotiling
    if (tile.structureType === StructureType.ROAD) {
      const variation = this.getRoadVariation(tile.x, tile.y);
      const img = this.roadImages.get(variation);

      if (img && img.complete && img.naturalWidth > 0) {
        // Draw road image
        this.ctx.drawImage(img, screenX, screenY, this.tileSize, this.tileSize);
      } else {
        // Fallback to colored square if images not loaded
        this.ctx.fillStyle = '#c4a574';
        const roadPadding = this.tileSize * 0.15;
        this.ctx.fillRect(
          screenX + roadPadding,
          screenY + roadPadding,
          this.tileSize - roadPadding * 2,
          this.tileSize - roadPadding * 2
        );
      }
    }

    // Draw building
    if (tile.structureType === StructureType.BUILDING && tile.buildingType !== undefined) {
      this.drawBuilding(screenX, screenY, tile.buildingType, tile.ownerPlayerNumber);
    }

    // Draw ownership contour (only on edges where ownership differs from neighbor)
    if (tile.owners && tile.owners.length > 0) {
      this.drawOwnershipContour(tile.x, tile.y, screenX, screenY, tile.owners);
    }

    // Draw border
    this.ctx.strokeStyle = 'rgba(0, 0, 0, 0.3)';
    this.ctx.lineWidth = 1;
    this.ctx.strokeRect(screenX, screenY, this.tileSize, this.tileSize);
  }

  private drawOwnershipContour(tileX: number, tileY: number, screenX: number, screenY: number, owners: number[]): void {
    const contourWidth = 3;

    // For each owner, only draw border on sides where neighbor has different ownership
    for (const owner of owners) {
      // Look up the color index for this player number
      const colorIndex = this.playerColorMap.get(owner) ?? owner;
      this.ctx.strokeStyle = getPlayerColor(colorIndex);
      this.ctx.lineWidth = contourWidth;

      // Check each direction: N, E, S, W
      const directions = [
        { dx: 0, dy: -1, side: 'top' },    // North
        { dx: 1, dy: 0, side: 'right' },   // East
        { dx: 0, dy: 1, side: 'bottom' },  // South
        { dx: -1, dy: 0, side: 'left' }    // West
      ];

      for (const dir of directions) {
        const neighborTile = this.tiles.get(`${tileX + dir.dx},${tileY + dir.dy}`);
        const neighborOwners = neighborTile?.owners || [];

        // Draw border if neighbor doesn't have this owner
        if (!neighborOwners.includes(owner)) {
          this.ctx.beginPath();
          const offset = contourWidth / 2;

          switch (dir.side) {
            case 'top':
              this.ctx.moveTo(screenX, screenY + offset);
              this.ctx.lineTo(screenX + this.tileSize, screenY + offset);
              break;
            case 'right':
              this.ctx.moveTo(screenX + this.tileSize - offset, screenY);
              this.ctx.lineTo(screenX + this.tileSize - offset, screenY + this.tileSize);
              break;
            case 'bottom':
              this.ctx.moveTo(screenX, screenY + this.tileSize - offset);
              this.ctx.lineTo(screenX + this.tileSize, screenY + this.tileSize - offset);
              break;
            case 'left':
              this.ctx.moveTo(screenX + offset, screenY);
              this.ctx.lineTo(screenX + offset, screenY + this.tileSize);
              break;
          }
          this.ctx.stroke();
        }
      }
    }
  }

  private getRoadVariation(x: number, y: number): string {
    const n = this.hasRoad(x, y - 1) ? '1' : '0';
    const e = this.hasRoad(x + 1, y) ? '1' : '0';
    const s = this.hasRoad(x, y + 1) ? '1' : '0';
    const w = this.hasRoad(x - 1, y) ? '1' : '0';
    return n + e + s + w;
  }

  private hasRoad(x: number, y: number): boolean {
    const tile = this.tiles.get(`${x},${y}`);
    return tile?.structureType === StructureType.ROAD;
  }

  private drawBuilding(screenX: number, screenY: number, buildingType: number, ownerPlayerNumber?: number): void {
    const building = BUILDING_DEFINITIONS.find(b => b.id === buildingType);
    if (!building) return;

    // Draw building background
    this.ctx.fillStyle = 'rgba(100, 100, 100, 0.7)';
    const padding = this.tileSize * 0.1;
    this.ctx.fillRect(screenX + padding, screenY + padding, this.tileSize - padding * 2, this.tileSize - padding * 2);

    // Draw building icon
    this.ctx.font = `${this.tileSize * 0.5}px Arial`;
    this.ctx.textAlign = 'center';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillStyle = '#fff';
    this.ctx.fillText(building.icon, screenX + this.tileSize / 2, screenY + this.tileSize / 2);
  }

  private drawPlacementPreview(): void {
    const tile = this.tiles.get(`${this.hoverTileX},${this.hoverTileY}`);
    if (!tile || !this.placementMode) return;

    const screenX = this.hoverTileX * this.tileSize - this.cameraX;
    const screenY = this.hoverTileY * this.tileSize - this.cameraY;

    const canPlace = this.canPlaceBuilding(tile, this.placementMode);
    const color = canPlace ? 'rgba(0, 150, 255, 0.5)' : 'rgba(255, 0, 0, 0.5)';

    this.ctx.fillStyle = color;
    this.ctx.fillRect(screenX, screenY, this.tileSize, this.tileSize);

    // Draw building icon preview
    this.ctx.font = `${this.tileSize * 0.5}px Arial`;
    this.ctx.textAlign = 'center';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillStyle = canPlace ? '#fff' : '#ffaaaa';
    this.ctx.fillText(this.placementMode.icon, screenX + this.tileSize / 2, screenY + this.tileSize / 2);
  }

  private canPlaceBuilding(tile: Tile, building: BuildingDefinition): boolean {
    // Check ownership - player can only build on their own territory
    if (!tile.owners || !tile.owners.includes(this.myPlayerNumber)) {
      return false;
    }
    // Check terrain
    if (!building.allowedTerrains.includes(tile.terrainType)) {
      return false;
    }
    // Check for existing structure
    if (tile.structureType !== undefined) {
      return false;
    }
    // Check resources
    return this.resources.wood >= building.cost.wood &&
           this.resources.stone >= building.cost.stone &&
           this.resources.gold >= building.cost.gold;
  }

  private drawGrid(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    this.ctx.lineWidth = 1;

    const startX = -(this.cameraX % this.tileSize);
    for (let x = startX; x < canvas.width; x += this.tileSize) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, canvas.height);
      this.ctx.stroke();
    }

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
    this.hasDragged = false; // Always reset drag state on mouse down
    if (this.placementMode) return; // Don't drag in placement mode
    this.isDragging = true;
    this.lastMouseX = event.clientX;
    this.lastMouseY = event.clientY;
  }

  onMouseMove(event: MouseEvent): void {
    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    // Update hover tile for placement preview
    if (this.placementMode) {
      const tileX = Math.floor((this.cameraX + mouseX) / this.tileSize);
      const tileY = Math.floor((this.cameraY + mouseY) / this.tileSize);
      if (tileX !== this.hoverTileX || tileY !== this.hoverTileY) {
        this.hoverTileX = tileX;
        this.hoverTileY = tileY;
        this.render();
      }
    }

    if (!this.isDragging) return;

    const deltaX = event.clientX - this.lastMouseX;
    const deltaY = event.clientY - this.lastMouseY;

    if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
      this.hasDragged = true;
    }

    this.cameraX -= deltaX;
    this.cameraY -= deltaY;

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

  getMyPlayerColor(): string {
    return getPlayerColor(this.myColorIndex);
  }

  // Selection
  private drawSelection(): void {
    if (!this.selectedTile) return;

    const screenX = this.selectedTile.x * this.tileSize - this.cameraX;
    const screenY = this.selectedTile.y * this.tileSize - this.cameraY;

    this.ctx.strokeStyle = '#00d9ff';
    this.ctx.lineWidth = 3;
    this.ctx.strokeRect(screenX + 1, screenY + 1, this.tileSize - 2, this.tileSize - 2);

    const markerSize = 6;
    this.ctx.fillStyle = '#00d9ff';
    this.ctx.fillRect(screenX, screenY, markerSize, 3);
    this.ctx.fillRect(screenX, screenY, 3, markerSize);
    this.ctx.fillRect(screenX + this.tileSize - markerSize, screenY, markerSize, 3);
    this.ctx.fillRect(screenX + this.tileSize - 3, screenY, 3, markerSize);
    this.ctx.fillRect(screenX, screenY + this.tileSize - 3, markerSize, 3);
    this.ctx.fillRect(screenX, screenY + this.tileSize - markerSize, 3, markerSize);
    this.ctx.fillRect(screenX + this.tileSize - markerSize, screenY + this.tileSize - 3, markerSize, 3);
    this.ctx.fillRect(screenX + this.tileSize - 3, screenY + this.tileSize - markerSize, 3, markerSize);
  }

  onCanvasClick(event: MouseEvent): void {
    if (this.hasDragged) return;

    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    const tileX = Math.floor((this.cameraX + mouseX) / this.tileSize);
    const tileY = Math.floor((this.cameraY + mouseY) / this.tileSize);

    // Handle placement mode
    if (this.placementMode) {
      const tile = this.tiles.get(`${tileX},${tileY}`);
      if (tile && this.canPlaceBuilding(tile, this.placementMode)) {
        // Send place building command
        this.socketService.sendCommand('GAME', 'PLACE_BUILDING', {
          x: tileX,
          y: tileY,
          buildingType: this.placementMode.id
        });
        this.cancelPlacementMode();
      } else {
        this.showError('Cannot place building here');
      }
      return;
    }

    // Normal selection
    const tile = this.tiles.get(`${tileX},${tileY}`);
    if (tile) {
      this.selectedTile = tile;
      this.selectedTileX = tileX;
      this.selectedTileY = tileY;
      this.render();
    }
  }

  // Building placement
  onBuildingSelected(building: BuildingDefinition): void {
    this.placementMode = building;
    this.selectedTile = null;
    this.selectedTileX = -1;
    this.selectedTileY = -1;
  }

  cancelPlacementMode(): void {
    this.placementMode = null;
    this.hoverTileX = -1;
    this.hoverTileY = -1;
    this.render();
  }

  // Action Panel Handler
  onActionTriggered(action: string): void {
    console.log('Action triggered:', action);
    if (action === 'createTurret') {
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
