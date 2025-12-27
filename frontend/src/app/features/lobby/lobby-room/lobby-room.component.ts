import { Component, OnInit, effect, computed, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LobbyService, AuthService, SocketService } from '../../../core/services';
import { ChatComponent } from '../../../shared/components/chat/chat.component';
import { PLAYER_COLORS } from '../../../shared/constants/player-colors';

const COLOR_NAMES: string[] = [
  'Red', 'Blue', 'Green', 'Orange', 'Purple', 'Cyan',
  'Amber', 'Pink', 'Deep Purple', 'Teal', 'Light Green',
  'Deep Orange', 'Indigo', 'Lime', 'Brown', 'Blue Grey'
];

@Component({
  selector: 'app-lobby-room',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatComponent],
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

            <div class="players-card">
              <h2>Players</h2>
              @for (player of lobbyService.players(); track player.id) {
                <div class="player-row">
                  <div class="player-color" [style.background-color]="getPlayerColor(player.colorIndex)"></div>
                  <span class="player-name">{{ player.name }}</span>
                  @if (player.id === myId()) {
                    <div class="color-picker" [class.disabled]="player.ready">
                      <button
                        class="color-dropdown-trigger"
                        [disabled]="player.ready"
                        (click)="toggleColorDropdown()">
                        <span class="color-swatch" [style.background-color]="getPlayerColor(player.colorIndex)"></span>
                        <span class="color-label">{{ getColorName(player.colorIndex) }}</span>
                        <span class="dropdown-arrow">▼</span>
                      </button>
                      @if (colorDropdownOpen && !player.ready) {
                        <div class="color-dropdown">
                          @for (color of availableColors(player.colorIndex); track color.index) {
                            <button
                              class="color-option"
                              [class.selected]="color.index === player.colorIndex"
                              (click)="selectColor(color.index)">
                              <span class="color-swatch" [style.background-color]="getPlayerColor(color.index)"></span>
                              <span>{{ color.name }}</span>
                            </button>
                          }
                        </div>
                      }
                    </div>
                    <button
                      class="ready-btn"
                      [class.ready]="player.ready"
                      (click)="toggleReady()">
                      @if (player.ready) {
                        <span class="ready-icon">✓</span> Ready
                      } @else {
                        Not Ready
                      }
                    </button>
                  } @else {
                    <span class="color-name">{{ getColorName(player.colorIndex) }}</span>
                    @if (player.ready) {
                      <span class="ready-badge">Ready</span>
                    } @else {
                      <span class="not-ready-badge">Not Ready</span>
                    }
                  }
                </div>
              }
            </div>

            <div class="actions">
              @if (isHost()) {
                <button
                  class="btn-primary btn-start"
                  [disabled]="!allPlayersReady()"
                  (click)="startGame()">
                  Start Game
                </button>
                @if (!allPlayersReady()) {
                  <p class="waiting-text">Waiting for all players to ready up...</p>
                }
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

    .lobby-info-card, .players-card {
      background: #0f0f23;
      padding: 1.5rem;
      border-radius: 8px;
      border: 1px solid #333;
    }

    .lobby-info-card h2, .players-card h2 {
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

    .player-row {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.75rem;
      background: #1a1a2e;
      border-radius: 4px;
      margin-bottom: 0.5rem;
    }

    .player-color {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      border: 2px solid #fff;
    }

    .player-name {
      color: #fff;
      font-weight: 500;
      flex: 1;
    }

    .color-picker {
      position: relative;
    }

    .color-picker.disabled {
      opacity: 0.6;
    }

    .color-dropdown-trigger {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.4rem 0.75rem;
      background: #16213e;
      color: #fff;
      border: 1px solid #444;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .color-dropdown-trigger:hover:not(:disabled) {
      border-color: #00d9ff;
      background: #1a2744;
    }

    .color-dropdown-trigger:disabled {
      cursor: not-allowed;
      opacity: 0.7;
    }

    .color-swatch {
      width: 16px;
      height: 16px;
      border-radius: 3px;
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    .color-label {
      font-size: 0.875rem;
    }

    .dropdown-arrow {
      font-size: 0.6rem;
      opacity: 0.7;
      margin-left: 0.25rem;
    }

    .color-dropdown {
      position: absolute;
      top: 100%;
      left: 0;
      z-index: 100;
      margin-top: 4px;
      background: #1a1a2e;
      border: 1px solid #444;
      border-radius: 8px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
      max-height: 240px;
      overflow-y: auto;
      min-width: 160px;
    }

    .color-option {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      padding: 0.6rem 0.75rem;
      background: transparent;
      border: none;
      color: #fff;
      cursor: pointer;
      text-align: left;
      transition: background 0.15s ease;
    }

    .color-option:hover {
      background: #2a2a4e;
    }

    .color-option.selected {
      background: #00d9ff22;
    }

    .color-option .color-swatch {
      width: 20px;
      height: 20px;
    }

    .color-name {
      color: #aaa;
      font-size: 0.875rem;
    }

    .ready-btn {
      padding: 0.5rem 1rem;
      background: #444;
      color: #ccc;
      border: 2px solid #555;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.875rem;
      transition: all 0.2s ease;
      min-width: 110px;
    }

    .ready-btn:hover {
      background: #555;
      border-color: #666;
    }

    .ready-btn.ready {
      background: #2d5a2d;
      border-color: #4caf50;
      color: #8eff8e;
    }

    .ready-btn.ready:hover {
      background: #3d6a3d;
    }

    .ready-icon {
      font-weight: bold;
    }

    .ready-badge {
      padding: 0.35rem 0.75rem;
      background: linear-gradient(135deg, #4caf50, #45a049);
      color: #fff;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: bold;
      box-shadow: 0 2px 4px rgba(76, 175, 80, 0.3);
    }

    .not-ready-badge {
      padding: 0.35rem 0.75rem;
      background: #3a3a4e;
      color: #888;
      border-radius: 6px;
      font-size: 0.8rem;
    }

    .actions {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 2rem;
      gap: 1rem;
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

    .btn-primary:hover:not(:disabled) {
      background: #00b8d9;
    }

    .btn-primary:disabled {
      background: #555;
      color: #999;
      cursor: not-allowed;
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
  myId = computed(() => this.authService.user()?.id);
  colorDropdownOpen = false;

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
    // If not in a lobby, redirect back (unless session was invalidated)
    if (!this.lobbyService.activeLobby()) {
      setTimeout(() => {
        // Don't redirect if session was invalidated (logout in progress)
        if (!this.authService.isSessionInvalidated() && !this.lobbyService.activeLobby()) {
          this.router.navigate(['/lobby']);
        }
      }, 1000);
    }
  }

  isHost(): boolean {
    const lobby = this.lobbyService.activeLobby();
    const user = this.authService.user();
    return lobby?.hostId === user?.id || lobby?.id === user?.id;
  }

  getPlayerColor(colorIndex: number): string {
    return PLAYER_COLORS[colorIndex] || PLAYER_COLORS[0];
  }

  getColorName(colorIndex: number): string {
    return COLOR_NAMES[colorIndex] || 'Unknown';
  }

  availableColors(currentColorIndex: number): { index: number; name: string }[] {
    const players = this.lobbyService.players();
    const myId = this.authService.user()?.id;
    const takenColors = players
      .filter(p => p.id !== myId)
      .map(p => p.colorIndex);

    return COLOR_NAMES.map((name, index) => ({ index, name }))
      .filter(c => !takenColors.includes(c.index) || c.index === currentColorIndex);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.color-picker')) {
      this.colorDropdownOpen = false;
    }
  }

  toggleColorDropdown(): void {
    this.colorDropdownOpen = !this.colorDropdownOpen;
  }

  selectColor(colorIndex: number): void {
    this.lobbyService.changeColor(colorIndex);
    this.colorDropdownOpen = false;
  }

  changeColor(colorIndex: number): void {
    this.lobbyService.changeColor(colorIndex);
  }

  toggleReady(): void {
    this.lobbyService.toggleReady();
  }

  allPlayersReady(): boolean {
    const players = this.lobbyService.players();
    return players.length > 0 && players.every(p => p.ready);
  }

  startGame(): void {
    this.lobbyService.startGame();
  }

  leaveLobby(): void {
    this.lobbyService.leaveLobby();
    this.router.navigate(['/lobby']);
  }
}
