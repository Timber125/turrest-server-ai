export interface Lobby {
  id: string;
  size: number;
  hidden: boolean;
  password?: string;
  game: string;
}

export interface CreateLobbyRequest {
  size: number;
  hidden: boolean;
  password: string;
  game: string;
}

export interface JoinLobbyRequest {
  lobby_ID: string;
  password: string;
}
