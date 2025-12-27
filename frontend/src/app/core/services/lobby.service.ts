import { Injectable, signal } from '@angular/core';
import { SocketService } from './socket.service';
import { Lobby, CreateLobbyRequest, ClientSocketSubject, SocketTopic, ServerSocketSubject } from '../../shared/models';

@Injectable({
  providedIn: 'root'
})
export class LobbyService {
  private lobbies = signal<Lobby[]>([]);
  private currentLobby = signal<Lobby | null>(null);
  private inGame = signal<boolean>(false);

  readonly lobbiesList = this.lobbies.asReadonly();
  readonly activeLobby = this.currentLobby.asReadonly();
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
  }

  private parseLobbiesResponse(data: Record<string, any>): Lobby[] {
    const lobbies: Lobby[] = [];
    const count = data['numberOfLobbies'] || 0;

    for (let i = 0; i < count; i++) {
      lobbies.push({
        id: data[`lobby_id${i}`],
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
      game: request.game
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
    this.currentLobby.set(null);
    this.inGame.set(false);
  }
}
