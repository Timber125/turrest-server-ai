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
  splashRadius?: number;
  slowFactor?: number;
  slowDurationMs?: number;
  description?: string;
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
    damage: 30,
    description: 'Balanced tower with decent range and damage'
  },
  {
    id: 2,
    name: 'Sniper Tower',
    icon: '🎯',
    cost: { wood: 60, stone: 150, gold: 200 },
    allowedTerrains: [TerrainType.GRASS, TerrainType.DIRT],
    range: 5,
    cooldownMs: 3000,
    damage: 80,
    description: 'Long range, high damage, slow fire rate'
  },
  {
    id: 3,
    name: 'Splash Tower',
    icon: '💥',
    cost: { wood: 120, stone: 120, gold: 150 },
    allowedTerrains: [TerrainType.GRASS, TerrainType.DIRT],
    range: 2.5,
    cooldownMs: 1500,
    damage: 20,
    splashRadius: 1.0,
    description: 'Area damage - hits all creeps in splash radius'
  },
  {
    id: 4,
    name: 'Slow Tower',
    icon: '❄️',
    cost: { wood: 60, stone: 100, gold: 130 },
    allowedTerrains: [TerrainType.GRASS, TerrainType.DIRT],
    range: 2.5,
    cooldownMs: 800,
    damage: 15,
    slowFactor: 0.5,
    slowDurationMs: 2500,
    description: 'Slows enemies by 50% for 2.5 seconds'
  },
  {
    id: 5,
    name: 'Rapid Tower',
    icon: '⚡',
    cost: { wood: 100, stone: 80, gold: 100 },
    allowedTerrains: [TerrainType.GRASS, TerrainType.DIRT],
    range: 2,
    cooldownMs: 400,
    damage: 12,
    description: 'Very fast fire rate, low damage per shot'
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

// Creep definitions for sending creeps to opponents
export interface CreepDefinition {
  id: string;
  name: string;
  icon: string;
  sendCost: { wood: number; stone: number; gold: number };
  hitpoints: number;
  speed: number;
  description?: string;
  spawnCount?: number;
}

export const CREEP_DEFINITIONS: CreepDefinition[] = [
  {
    id: 'RUNNER',
    name: 'Runner',
    icon: '🏃',
    sendCost: { wood: 0, stone: 0, gold: 5 },
    hitpoints: 25,
    speed: 20,
    description: 'Very fast but fragile'
  },
  {
    id: 'GHOST',
    name: 'Ghost',
    icon: '👻',
    sendCost: { wood: 0, stone: 0, gold: 10 },
    hitpoints: 50,
    speed: 30,
    description: 'Balanced speed and health'
  },
  {
    id: 'SWARM',
    name: 'Swarm',
    icon: '🐜',
    sendCost: { wood: 0, stone: 0, gold: 8 },
    hitpoints: 15,
    speed: 25,
    spawnCount: 5,
    description: 'Spawns 5 weak creeps'
  },
  {
    id: 'HEALER',
    name: 'Healer',
    icon: '💚',
    sendCost: { wood: 0, stone: 0, gold: 25 },
    hitpoints: 80,
    speed: 30,
    description: 'Heals nearby creeps'
  },
  {
    id: 'TROLL',
    name: 'Troll',
    icon: '🧌',
    sendCost: { wood: 0, stone: 0, gold: 30 },
    hitpoints: 250,
    speed: 25,
    description: 'Tough and resilient'
  },
  {
    id: 'TANK',
    name: 'Tank',
    icon: '🛡️',
    sendCost: { wood: 0, stone: 0, gold: 60 },
    hitpoints: 500,
    speed: 40,
    description: 'Massive HP, very slow'
  }
];
