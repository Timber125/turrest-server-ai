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

export interface Tile {
  x: number;
  y: number;
  terrainType: TerrainType;
  building?: Building;
}

export interface Building {
  name: string;
  type: number;
}

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
