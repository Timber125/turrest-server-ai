import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LobbyService, AuthService, SocketService, ChatService } from '../../../core/services';
import { Lobby } from '../../../shared/models';
import { ChatComponent } from '../../../shared/components/chat/chat.component';

@Component({
  selector: 'app-lobby-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatComponent],
  template: `
    <div class="lobby-container">
      <header class="lobby-header">
        <h1>TURREST</h1>
        <div class="user-info">
          <span>{{ authService.user()?.username }}</span>
          <button class="btn-logout" (click)="logout()">Logout</button>
        </div>
      </header>

      <div class="lobby-content">
        <div class="lobby-main">
          <div class="lobby-actions">
            <button class="btn-primary" (click)="refreshLobbies()">Refresh</button>
            <button class="btn-primary" (click)="showCreateDialog.set(true)">Create Lobby</button>
          </div>

          <div class="lobby-list">
            <h2>Available Lobbies</h2>
            @if (lobbyService.lobbiesList().length === 0) {
              <p class="no-lobbies">No lobbies available. Create one!</p>
            } @else {
              <div class="lobby-grid">
                @for (lobby of lobbyService.lobbiesList(); track lobby.id) {
                  <div class="lobby-card">
                    <div class="lobby-info">
                      <span class="lobby-name">{{ lobby.name || 'Unnamed Lobby' }}</span>
                      <span class="lobby-game">{{ lobby.game }}</span>
                      <span class="lobby-size">{{ lobby.size }} players</span>
                      @if (lobby.hidden) {
                        <span class="lobby-hidden">Private</span>
                      }
                    </div>
                    <button class="btn-join" (click)="joinLobby(lobby)">Join</button>
                  </div>
                }
              </div>
            }
          </div>
        </div>

        <div class="chat-sidebar">
          <app-chat></app-chat>
        </div>
      </div>

      @if (showCreateDialog()) {
        <div class="dialog-overlay" (click)="showCreateDialog.set(false)">
          <div class="dialog" (click)="$event.stopPropagation()">
            <h3>Create Lobby</h3>

            <div class="form-group">
              <label>Lobby Name (optional)</label>
              <input type="text" [(ngModel)]="newLobby.name" [ngModelOptions]="{standalone: true}" placeholder="My Lobby">
            </div>

            <div class="form-group">
              <label>Game Type</label>
              <select [(ngModel)]="newLobby.game" [ngModelOptions]="{standalone: true}">
                <option value="TURREST-mode1">TURREST Mode 1</option>
              </select>
            </div>

            <div class="form-group">
              <label>Max Players</label>
              <select [(ngModel)]="newLobby.size" [ngModelOptions]="{standalone: true}">
                <option [value]="2">2</option>
                <option [value]="4">4</option>
                <option [value]="6">6</option>
                <option [value]="8">8</option>
              </select>
            </div>

            <div class="form-group">
              <label>
                <input type="checkbox" [(ngModel)]="newLobby.hidden" [ngModelOptions]="{standalone: true}">
                Private Lobby
              </label>
            </div>

            @if (newLobby.hidden) {
              <div class="form-group">
                <label>Password</label>
                <input type="password" [(ngModel)]="newLobby.password" [ngModelOptions]="{standalone: true}">
              </div>
            }

            <div class="dialog-actions">
              <button class="btn-secondary" (click)="showCreateDialog.set(false)">Cancel</button>
              <button class="btn-primary" (click)="createLobby()">Create</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .lobby-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
      display: flex;
      flex-direction: column;
    }

    .lobby-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem;
      background: #0f0f23;
      border-bottom: 1px solid #333;
    }

    .lobby-header h1 {
      color: #00d9ff;
      margin: 0;
      letter-spacing: 4px;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 1rem;
      color: #fff;
    }

    .btn-logout {
      padding: 0.5rem 1rem;
      background: transparent;
      border: 1px solid #ff4444;
      color: #ff4444;
      border-radius: 4px;
      cursor: pointer;
    }

    .btn-logout:hover {
      background: #ff4444;
      color: #fff;
    }

    .lobby-content {
      display: flex;
      flex: 1;
      padding: 1rem;
      gap: 1rem;
    }

    .lobby-main {
      flex: 1;
    }

    .lobby-actions {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .btn-primary {
      padding: 0.75rem 1.5rem;
      background: #00d9ff;
      border: none;
      color: #000;
      border-radius: 4px;
      cursor: pointer;
      font-weight: bold;
    }

    .btn-primary:hover {
      background: #00b8d9;
    }

    .lobby-list h2 {
      color: #fff;
      margin-bottom: 1rem;
    }

    .no-lobbies {
      color: #aaa;
      text-align: center;
      padding: 2rem;
    }

    .lobby-grid {
      display: grid;
      gap: 1rem;
    }

    .lobby-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem;
      background: #0f0f23;
      border-radius: 8px;
      border: 1px solid #333;
    }

    .lobby-info {
      display: flex;
      gap: 1rem;
      align-items: center;
    }

    .lobby-name {
      color: #fff;
      font-weight: bold;
      font-size: 1.1rem;
    }

    .lobby-game {
      color: #00d9ff;
      font-size: 0.9rem;
    }

    .lobby-size {
      color: #aaa;
    }

    .lobby-hidden {
      background: #444;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.8rem;
      color: #fff;
    }

    .btn-join {
      padding: 0.5rem 1rem;
      background: #00d9ff;
      border: none;
      color: #000;
      border-radius: 4px;
      cursor: pointer;
    }

    .chat-sidebar {
      width: 300px;
    }

    .dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 100;
    }

    .dialog {
      background: #0f0f23;
      padding: 2rem;
      border-radius: 8px;
      width: 100%;
      max-width: 400px;
    }

    .dialog h3 {
      color: #fff;
      margin-bottom: 1.5rem;
    }

    .form-group {
      margin-bottom: 1rem;
    }

    .form-group label {
      display: block;
      color: #aaa;
      margin-bottom: 0.5rem;
    }

    .form-group input,
    .form-group select {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #333;
      border-radius: 4px;
      background: #1a1a2e;
      color: #fff;
      box-sizing: border-box;
    }

    .form-group input[type="checkbox"] {
      width: auto;
      margin-right: 0.5rem;
    }

    .dialog-actions {
      display: flex;
      gap: 1rem;
      margin-top: 1.5rem;
    }

    .btn-secondary {
      padding: 0.75rem 1.5rem;
      background: transparent;
      border: 1px solid #666;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
      flex: 1;
    }

    .dialog-actions .btn-primary {
      flex: 1;
    }
  `]
})
export class LobbyListComponent implements OnInit, OnDestroy {
  showCreateDialog = signal(false);
  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  newLobby = {
    name: '',
    game: 'TURREST-mode1',
    size: 4,
    hidden: false,
    password: ''
  };

  constructor(
    public lobbyService: LobbyService,
    public authService: AuthService,
    private socketService: SocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Connect to socket if not connected
    if (this.socketService.state() !== 'connected') {
      this.socketService.connect();
    }

    // Subscribe to lobby changes
    this.lobbyService.activeLobby;

    // Refresh lobbies on init, then start auto-refresh interval
    setTimeout(() => {
      this.refreshLobbies();
      // Start auto-refresh every 5 seconds after initial refresh
      this.refreshInterval = setInterval(() => this.refreshLobbies(), 5000);
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  refreshLobbies(): void {
    // Only refresh if socket is connected
    if (this.socketService.state() === 'connected') {
      this.lobbyService.refreshLobbies();
    }
  }

  createLobby(): void {
    this.lobbyService.createLobby({
      size: Number(this.newLobby.size),
      hidden: this.newLobby.hidden,
      password: this.newLobby.password,
      game: this.newLobby.game,
      name: this.newLobby.name || undefined
    });
    this.showCreateDialog.set(false);
    this.router.navigate(['/lobby/room']);
  }

  joinLobby(lobby: Lobby): void {
    this.lobbyService.joinLobby(lobby.id);
    this.router.navigate(['/lobby/room']);
  }

  logout(): void {
    this.socketService.disconnect();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
