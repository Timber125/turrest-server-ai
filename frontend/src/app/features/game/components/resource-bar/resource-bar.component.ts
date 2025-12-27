import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerResources } from '../../../../shared/models';

@Component({
  selector: 'app-resource-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="resource-bar">
      <div class="resource wood">
        <span class="resource-icon">🪵</span>
        <span class="resource-value">{{ resources.wood }}</span>
      </div>
      <div class="resource stone">
        <span class="resource-icon">🪨</span>
        <span class="resource-value">{{ resources.stone }}</span>
      </div>
      <div class="resource gold">
        <span class="resource-icon">🪙</span>
        <span class="resource-value">{{ resources.gold }}</span>
      </div>
    </div>
  `,
  styles: [`
    .resource-bar {
      display: flex;
      gap: 1.5rem;
      padding: 0.5rem 1rem;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 8px;
      border: 1px solid #333;
    }

    .resource {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }

    .resource-icon {
      font-size: 1.2rem;
    }

    .resource-value {
      color: #fff;
      font-weight: bold;
      font-size: 1rem;
      min-width: 40px;
    }

    .wood .resource-value {
      color: #8B4513;
    }

    .stone .resource-value {
      color: #A9A9A9;
    }

    .gold .resource-value {
      color: #FFD700;
    }
  `]
})
export class ResourceBarComponent {
  @Input() resources: PlayerResources = { wood: 0, stone: 0, gold: 0 };
}
