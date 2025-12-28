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
  playerNumber: number;
  hitpoints: number;
  maxHitpoints: number;
  speed: number;          // Tiles per second
}
