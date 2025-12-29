export interface LobbyPlayer {
  id: string;
  name: string;
  colorIndex: number;
  ready: boolean;
  isBot?: boolean;
  botDifficulty?: string | null;
}

export interface Lobby {
  id: string;
  name?: string;
  size: number;
  hidden: boolean;
  password?: string;
  game: string;
  hostId?: string;
  players?: LobbyPlayer[];
  allReady?: boolean;
}

export interface CreateLobbyRequest {
  size: number;
  hidden: boolean;
  password: string;
  game: string;
  name?: string;
}

export interface RenameLobbyRequest {
  name: string;
}

export interface JoinLobbyRequest {
  lobby_ID: string;
  password: string;
}
