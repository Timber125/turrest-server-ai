import { Component, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LobbyService, AuthService, SocketService } from '../../../core/services';
import { ChatComponent } from '../../../shared/components/chat/chat.component';

@Component({
  selector: 'app-lobby-room',
  standalone: true,
  imports: [CommonModule, ChatComponent],
  template: `
    <div class="room-container">
      <header class="room-header">
        <button class="btn-back" (click)="leaveLobby()">← Back to Lobbies</button>
        <h1>{{ lobbyService.activeLobby()?.game || 'Lobby' }}</h1>
        <div class="user-info">
          <span>{{ authService.user()?.username }}</span>
        </div>
      </header>

      <div class="room-content">
        <div class="room-main">
          @if (lobbyService.activeLobby(); as lobby) {
            <div class="lobby-info-card">
              <h2>Lobby Info</h2>
              <p><strong>Game:</strong> {{ lobby.game }}</p>
              <p><strong>Max Players:</strong> {{ lobby.size }}</p>
              <p><strong>Status:</strong> {{ lobby.hidden ? 'Private' : 'Public' }}</p>
            </div>

            <div class="actions">
              @if (isHost()) {
                <button class="btn-primary btn-start" (click)="startGame()">
                  Start Game
                </button>
              } @else {
                <p class="waiting-text">Waiting for host to start the game...</p>
              }
            </div>
          } @else {
            <div class="no-lobby">
              <p>You are not in a lobby.</p>
              <button class="btn-primary" (click)="leaveLobby()">Back to Lobbies</button>
            </div>
          }
        </div>

        <div class="chat-sidebar">
          <app-chat></app-chat>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .room-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
      display: flex;
      flex-direction: column;
    }

    .room-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem;
      background: #0f0f23;
      border-bottom: 1px solid #333;
    }

    .room-header h1 {
      color: #00d9ff;
      margin: 0;
    }

    .btn-back {
      padding: 0.5rem 1rem;
      background: transparent;
      border: 1px solid #666;
      color: #fff;
      border-radius: 4px;
      cursor: pointer;
    }

    .btn-back:hover {
      background: #333;
    }

    .user-info {
      color: #fff;
    }

    .room-content {
      display: flex;
      flex: 1;
      padding: 1rem;
      gap: 1rem;
    }

    .room-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .lobby-info-card {
      background: #0f0f23;
      padding: 1.5rem;
      border-radius: 8px;
      border: 1px solid #333;
    }

    .lobby-info-card h2 {
      color: #00d9ff;
      margin-bottom: 1rem;
    }

    .lobby-info-card p {
      color: #fff;
      margin: 0.5rem 0;
    }

    .lobby-info-card strong {
      color: #aaa;
    }

    .actions {
      display: flex;
      justify-content: center;
      padding: 2rem;
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

    .btn-start {
      padding: 1rem 3rem;
      font-size: 1.2rem;
    }

    .btn-primary:hover {
      background: #00b8d9;
    }

    .waiting-text {
      color: #aaa;
      font-style: italic;
    }

    .no-lobby {
      text-align: center;
      padding: 2rem;
      color: #aaa;
    }

    .no-lobby .btn-primary {
      margin-top: 1rem;
    }

    .chat-sidebar {
      width: 300px;
    }
  `]
})
export class LobbyRoomComponent implements OnInit {
  constructor(
    public lobbyService: LobbyService,
    public authService: AuthService,
    private socketService: SocketService,
    private router: Router
  ) {
    // Navigate to game when game starts
    effect(() => {
      if (this.lobbyService.isInGame()) {
        this.router.navigate(['/game']);
      }
    });
  }

  ngOnInit(): void {
    // If not in a lobby, redirect back
    if (!this.lobbyService.activeLobby()) {
      // Give a moment for the lobby to be set from joining
      setTimeout(() => {
        if (!this.lobbyService.activeLobby()) {
          this.router.navigate(['/lobby']);
        }
      }, 1000);
    }
  }

  isHost(): boolean {
    const lobby = this.lobbyService.activeLobby();
    const user = this.authService.user();
    return lobby?.id === user?.id;
  }

  startGame(): void {
    this.lobbyService.startGame();
  }

  leaveLobby(): void {
    this.lobbyService.leaveLobby();
    this.router.navigate(['/lobby']);
  }
}
