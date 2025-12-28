export enum TerrainType {
  GRASS = 1,
  DIRT = 2,
  FOREST = 3,
  WATER_SHALLOW = 4,
  WATER_DEEP = 5,
  ROCKY = 6,
  CASTLE = 7,
  SPAWNER = 8
}

export enum StructureType {
  ROAD = 0,
  BUILDING = 1
}

export interface Tile {
  x: number;
  y: number;
  terrainType: TerrainType;
  structureType?: StructureType;
  buildingType?: number;
  ownerPlayerNumber?: number;  // For buildings
  owners?: number[];           // Players who can build here
}

export interface Building {
  name: string;
  type: number;
}

export interface PlayerResources {
  wood: number;
  stone: number;
  gold: number;
}

export interface BuildingDefinition {
  id: number;
  name: string;
  icon: string;
  cost: { wood: number; stone: number; gold: number };
  allowedTerrains: TerrainType[];
}

export const BUILDING_DEFINITIONS: BuildingDefinition[] = [
  {
    id: 1,
    name: 'Lumbercamp',
    icon: '🪓',
    cost: { wood: 50, stone: 10, gold: 10 },
    allowedTerrains: [TerrainType.FOREST]
  },
  {
    id: 2,
    name: 'Stone Quarry',
    icon: '⛏️',
    cost: { wood: 10, stone: 50, gold: 10 },
    allowedTerrains: [TerrainType.ROCKY]
  },
  {
    id: 3,
    name: 'Gold Mine',
    icon: '⚒️',
    cost: { wood: 10, stone: 10, gold: 50 },
    allowedTerrains: [TerrainType.DIRT]
  }
];

export interface GameState {
  gameId: string;
  map: Tile[][];
  players: GamePlayer[];
}

export interface GamePlayer {
  id: string;
  name: string;
  playerNumber: number;
}

export interface Creep {
  id: string;
  creepType: string;
  x: number;              // Current rendered position (interpolated)
  y: number;
  targetX: number;        // Server-sent position (destination)
  targetY: number;
  playerNumber: number;   // Target player (whose castle the creep is heading to)
  spawnedByPlayer: number | null;  // null = wave-spawned, number = sent by that player
  hitpoints: number;
  maxHitpoints: number;
  speed: number;          // Tiles per second
}

export interface PlayerScoreEntry {
  playerNumber: number;
  colorIndex: number;
  username: string;
  score: number;
  isAlive: boolean;
}

// Tower types
export interface TowerDefinition {
  id: number;
  name: string;
  icon: string;
  cost: { wood: number; stone: number; gold: number };
  allowedTerrains: TerrainType[];
  range: number;
  cooldownMs: number;
  damage: number;
}

export const TOWER_DEFINITIONS: TowerDefinition[] = [
  {
    id: 1,
    name: 'Basic Tower',
    icon: '🗼',
    cost: { wood: 80, stone: 80, gold: 100 },
    allowedTerrains: [TerrainType.GRASS, TerrainType.DIRT],
    range: 3,
    cooldownMs: 1000,
    damage: 10
  }
];

export interface Tower {
  id: string;
  towerType: number;
  towerName: string;
  x: number;
  y: number;
  playerNumber: number;
  range: number;
  damage: number;
  cooldownMs: number;
  theoreticalFireRate: number;
  practicalFireRate: number;
}

export interface TowerAttack {
  towerId: string;
  towerX: number;
  towerY: number;
  targetCreepId: string;
  targetX: number;
  targetY: number;
  damage: number;
  bulletType: string;
  progress: number; // 0-1 for animation interpolation
}
