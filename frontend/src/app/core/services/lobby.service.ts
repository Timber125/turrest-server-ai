import { Injectable, signal } from '@angular/core';
import { SocketService } from './socket.service';
import { Lobby, LobbyPlayer, CreateLobbyRequest, ClientSocketSubject, SocketTopic, ServerSocketSubject } from '../../shared/models';

@Injectable({
  providedIn: 'root'
})
export class LobbyService {
  private lobbies = signal<Lobby[]>([]);
  private currentLobby = signal<Lobby | null>(null);
  private lobbyPlayers = signal<LobbyPlayer[]>([]);
  private inGame = signal<boolean>(false);

  readonly lobbiesList = this.lobbies.asReadonly();
  readonly activeLobby = this.currentLobby.asReadonly();
  readonly players = this.lobbyPlayers.asReadonly();
  readonly isInGame = this.inGame.asReadonly();

  constructor(private socketService: SocketService) {
    this.setupListeners();
  }

  private setupListeners(): void {
    // Listen for lobby list response
    this.socketService.onCommand(ServerSocketSubject.LOBBY, SocketTopic.DATA_ALL_LOBBIES)
      .subscribe(cmd => {
        const lobbies = this.parseLobbiesResponse(cmd.data);
        this.lobbies.set(lobbies);
      });

    // Listen for lobby created response
    this.socketService.onCommand(ServerSocketSubject.LOBBY, SocketTopic.CREATED)
      .subscribe(cmd => {
        const lobby: Lobby = {
          id: cmd.data['lobbyId'],
          size: cmd.data['size'],
          hidden: cmd.data['hidden'],
          password: cmd.data['password'],
          game: cmd.data['game']
        };
        this.currentLobby.set(lobby);
      });

    // Listen for connected to lobby response
    this.socketService.onCommand(ServerSocketSubject.LOBBY, SocketTopic.CONNECTED)
      .subscribe(cmd => {
        const lobby: Lobby = {
          id: cmd.data['lobbyId'],
          size: cmd.data['size'],
          hidden: cmd.data['hidden'],
          password: cmd.data['password'],
          game: cmd.data['game']
        };
        this.currentLobby.set(lobby);
      });

    // Listen for game started
    this.socketService.onCommand(ServerSocketSubject.LOBBY, SocketTopic.GAME_STARTED)
      .subscribe(() => {
        this.inGame.set(true);
      });

    // Listen for lobby state updates
    this.socketService.onCommand(ServerSocketSubject.LOBBY, 'LOBBY_STATE')
      .subscribe(cmd => {
        const players: LobbyPlayer[] = (cmd.data['players'] || []).map((p: any) => ({
          id: p.id,
          name: p.name,
          colorIndex: p.colorIndex,
          ready: p.ready,
          isBot: p.isBot || false,
          botDifficulty: p.botDifficulty || null
        }));
        this.lobbyPlayers.set(players);

        // Update lobby with additional info
        const currentLobby = this.currentLobby();
        if (currentLobby) {
          this.currentLobby.set({
            ...currentLobby,
            name: cmd.data['name'],
            hostId: cmd.data['hostId'],
            players: players,
            allReady: cmd.data['allReady']
          });
        }
      });
  }

  private parseLobbiesResponse(data: Record<string, any>): Lobby[] {
    const lobbies: Lobby[] = [];
    const count = data['numberOfLobbies'] || 0;

    for (let i = 0; i < count; i++) {
      lobbies.push({
        id: data[`lobby_id${i}`],
        name: data[`name${i}`],
        size: data[`size${i}`],
        hidden: data[`hidden${i}`],
        password: data[`password${i}`],
        game: data[`game${i}`]
      });
    }

    return lobbies;
  }

  refreshLobbies(): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, SocketTopic.GET_ALL);
  }

  createLobby(request: CreateLobbyRequest): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, SocketTopic.CREATE, {
      size: request.size,
      hidden: request.hidden,
      password: request.password,
      game: request.game,
      name: request.name
    });
  }

  joinLobby(lobbyId: string, password: string = ''): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, SocketTopic.JOIN, {
      lobby_ID: lobbyId,
      password: password
    });
  }

  startGame(): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, SocketTopic.START_GAME);
  }

  leaveLobby(): void {
    // Notify server that we're leaving the lobby
    if (this.currentLobby()) {
      this.socketService.sendCommand(ClientSocketSubject.LOBBY, SocketTopic.LEAVE, {});
    }
    this.currentLobby.set(null);
    this.lobbyPlayers.set([]);
    this.inGame.set(false);
  }

  leaveGame(): void {
    this.socketService.sendCommand(ClientSocketSubject.GAME, SocketTopic.LEAVE_GAME);
    this.leaveLobby();
  }

  changeColor(colorIndex: number): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, 'CHANGE_COLOR', { colorIndex });
  }

  toggleReady(): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, 'TOGGLE_READY', {});
  }

  renameLobby(name: string): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, 'RENAME', { name });
  }

  addBot(difficulty: string = 'EASY'): void {
    console.log('LobbyService.addBot() called with difficulty:', difficulty);
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, 'ADD_BOT', { difficulty });
  }

  removeBot(botId: string): void {
    this.socketService.sendCommand(ClientSocketSubject.LOBBY, 'REMOVE_BOT', { botId });
  }
}
