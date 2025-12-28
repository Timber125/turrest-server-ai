import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerScoreEntry } from '../../../../shared/models';
import { getPlayerColor } from '../../../../shared/constants/player-colors';

@Component({
  selector: 'app-ranklist',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ranklist">
      <h4>Rankings</h4>
      <div class="rank-entries">
        @for (entry of players; track entry.playerNumber; let i = $index) {
          <div class="rank-entry" [class.eliminated]="!entry.isAlive">
            <span class="rank">#{{ i + 1 }}</span>
            <span class="color-dot" [style.background]="getPlayerColor(entry.colorIndex)"></span>
            <span class="name">{{ entry.username }}</span>
            <span class="score">{{ entry.score }}</span>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .ranklist {
      background: #1a1a2e;
      border: 1px solid #333;
      border-radius: 8px;
      padding: 0.5rem;
      min-width: 160px;
    }

    h4 {
      margin: 0 0 0.5rem;
      color: #00d9ff;
      font-size: 0.9rem;
      text-align: center;
    }

    .rank-entries {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .rank-entry {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 4px 8px;
      background: #0f0f23;
      border-radius: 4px;
    }

    .rank-entry.eliminated {
      opacity: 0.5;
    }

    .rank-entry.eliminated .name,
    .rank-entry.eliminated .score {
      text-decoration: line-through;
    }

    .rank {
      color: #888;
      font-size: 0.75rem;
      width: 20px;
    }

    .color-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      border: 1px solid rgba(255, 255, 255, 0.5);
      flex-shrink: 0;
    }

    .name {
      color: #fff;
      flex: 1;
      font-size: 0.8rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .score {
      color: #00d9ff;
      font-weight: bold;
      font-size: 0.85rem;
    }
  `]
})
export class RanklistComponent {
  @Input() players: PlayerScoreEntry[] = [];

  getPlayerColor(colorIndex: number): string {
    return getPlayerColor(colorIndex);
  }
}
