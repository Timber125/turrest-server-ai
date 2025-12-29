import { Component, Output, EventEmitter, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SocketService } from '../../../../core/services';
import { Subscription } from 'rxjs';

interface PlayerStats {
  playerNumber: number;
  goldEarned: number;
  goldSpent: number;
  creepsKilled: number;
  creepsSent: number;
  damageDealt: number;
  damageTaken: number;
  buildingsPlaced: number;
  towersPlaced: number;
}

interface GameStats {
  gameDurationMs: number;
  players: PlayerStats[];
}

@Component({
  selector: 'app-stats-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stats-panel">
      <h2>Game Statistics</h2>

      @if (loading()) {
        <div class="loading">
          <span class="spinner"></span>
          Loading statistics...
        </div>
      } @else if (error()) {
        <div class="error">
          {{ error() }}
          <button class="retry-btn" (click)="loadStats()">Retry</button>
        </div>
      } @else if (stats()) {
        <div class="stats-content">
          <div class="game-duration">
            Game Duration: {{ formatDuration(stats()!.gameDurationMs) }}
          </div>

          @for (player of stats()!.players; track player.playerNumber) {
            <div class="player-stats">
              <h4>Player {{ player.playerNumber + 1 }}</h4>

              <div class="stats-grid">
                <div class="stat-item">
                  <span class="stat-icon gold">●</span>
                  <span class="stat-label">Gold Earned</span>
                  <span class="stat-value">{{ player.goldEarned | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon gold">●</span>
                  <span class="stat-label">Gold Spent</span>
                  <span class="stat-value">{{ player.goldSpent | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon kill">☠</span>
                  <span class="stat-label">Creeps Killed</span>
                  <span class="stat-value">{{ player.creepsKilled | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon send">➤</span>
                  <span class="stat-label">Creeps Sent</span>
                  <span class="stat-value">{{ player.creepsSent | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon damage">⚔</span>
                  <span class="stat-label">Damage Dealt</span>
                  <span class="stat-value">{{ player.damageDealt | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon damage">🛡</span>
                  <span class="stat-label">Damage Taken</span>
                  <span class="stat-value">{{ player.damageTaken | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon build">🏗</span>
                  <span class="stat-label">Buildings</span>
                  <span class="stat-value">{{ player.buildingsPlaced | number }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon tower">🗼</span>
                  <span class="stat-label">Towers</span>
                  <span class="stat-value">{{ player.towersPlaced | number }}</span>
                </div>
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="no-stats">
          <p>No statistics available yet.</p>
          <button class="refresh-btn" (click)="loadStats()">Refresh</button>
        </div>
      }

      <button class="back-btn" (click)="back.emit()">
        ← Back
      </button>
    </div>
  `,
  styles: [`
    .stats-panel {
      min-width: 400px;
    }

    h2 {
      color: #00d9ff;
      margin: 0 0 1rem;
      text-align: center;
      font-size: 1.3rem;
    }

    h4 {
      color: #fff;
      margin: 0 0 0.75rem;
      font-size: 1rem;
      border-bottom: 1px solid #333;
      padding-bottom: 0.5rem;
    }

    .loading {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      padding: 2rem;
      color: #888;
    }

    .spinner {
      width: 20px;
      height: 20px;
      border: 2px solid #333;
      border-top-color: #00d9ff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error {
      padding: 1rem;
      background: rgba(255, 68, 68, 0.1);
      border: 1px solid #ff4444;
      border-radius: 6px;
      color: #ff6666;
      text-align: center;
    }

    .retry-btn {
      margin-top: 0.75rem;
      padding: 0.4rem 1rem;
      background: transparent;
      border: 1px solid #ff4444;
      color: #ff6666;
      border-radius: 4px;
      cursor: pointer;
    }

    .retry-btn:hover {
      background: rgba(255, 68, 68, 0.2);
    }

    .stats-content {
      max-height: 350px;
      overflow-y: auto;
    }

    .game-duration {
      text-align: center;
      color: #00d9ff;
      font-size: 0.9rem;
      margin-bottom: 1rem;
      padding: 0.5rem;
      background: rgba(0, 217, 255, 0.1);
      border-radius: 4px;
    }

    .player-stats {
      background: #0f0f23;
      border-radius: 8px;
      padding: 1rem;
      margin-bottom: 1rem;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.5rem;
    }

    .stat-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.4rem 0.5rem;
      background: #1a1a2e;
      border-radius: 4px;
    }

    .stat-icon {
      font-size: 0.9rem;
      width: 20px;
      text-align: center;
    }

    .stat-icon.gold { color: #FFD700; }
    .stat-icon.kill { color: #ff6666; }
    .stat-icon.send { color: #66ff66; }
    .stat-icon.damage { color: #ff9966; }
    .stat-icon.build { color: #8B4513; }
    .stat-icon.tower { color: #888; }

    .stat-label {
      flex: 1;
      color: #aaa;
      font-size: 0.8rem;
    }

    .stat-value {
      color: #fff;
      font-weight: bold;
      font-size: 0.85rem;
    }

    .no-stats {
      text-align: center;
      padding: 2rem;
      color: #888;
    }

    .refresh-btn {
      margin-top: 0.75rem;
      padding: 0.5rem 1.5rem;
      background: transparent;
      border: 1px solid #00d9ff;
      color: #00d9ff;
      border-radius: 4px;
      cursor: pointer;
    }

    .refresh-btn:hover {
      background: rgba(0, 217, 255, 0.1);
    }

    .back-btn {
      width: 100%;
      padding: 0.75rem;
      background: transparent;
      border: 1px solid #444;
      color: #fff;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.9rem;
      margin-top: 1rem;
    }

    .back-btn:hover {
      background: #1f1f3f;
      border-color: #666;
    }
  `]
})
export class StatsPanelComponent implements OnInit, OnDestroy {
  @Output() back = new EventEmitter<void>();

  private readonly socketService = inject(SocketService);
  private statsSub: Subscription | null = null;

  loading = signal(false);
  error = signal<string | null>(null);
  stats = signal<GameStats | null>(null);

  ngOnInit(): void {
    // Listen for stats response
    this.statsSub = this.socketService.onCommand('GAME', 'STATS')
      .subscribe(cmd => {
        this.loading.set(false);
        this.error.set(null);
        this.stats.set(cmd.data as GameStats);
      });

    // Request stats on open
    this.loadStats();
  }

  ngOnDestroy(): void {
    this.statsSub?.unsubscribe();
  }

  loadStats(): void {
    this.loading.set(true);
    this.error.set(null);
    this.socketService.sendCommand('GAME', 'GET_STATS', {});

    // Timeout after 5 seconds
    setTimeout(() => {
      if (this.loading()) {
        this.loading.set(false);
        this.error.set('Request timed out');
      }
    }, 5000);
  }

  formatDuration(ms: number): string {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
      return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    }
    return `${seconds}s`;
  }
}
