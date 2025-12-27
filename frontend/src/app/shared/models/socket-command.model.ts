export interface SocketCommand {
  subject: string;
  topic: string;
  data: Record<string, any>;
}

// Client -> Server Subjects
export enum ClientSocketSubject {
  SOCKET_CONNECT = 'SOCKET_CONNECT',
  LOBBY = 'LOBBY',
  GLOBAL_CHAT = 'GLOBAL_CHAT'
}

// Server -> Client Subjects
export enum ServerSocketSubject {
  DISPLAY_CHAT = 'DISPLAY_CHAT',
  LOBBY = 'LOBBY'
}

// Topics
export enum SocketTopic {
  // Connection
  LOGIN = 'LOGIN',

  // Lobby
  GET_ALL = 'GET_ALL',
  CREATE = 'CREATE',
  JOIN = 'JOIN',
  START_GAME = 'START_GAME',

  // Lobby responses
  DATA_ALL_LOBBIES = 'DATA:ALL_LOBBIES',
  CREATED = 'CREATED',
  CONNECTED = 'CONNECTED',
  GAME_STARTED = 'GAME_STARTED',

  // Chat
  GLOBAL = 'GLOBAL'
}
