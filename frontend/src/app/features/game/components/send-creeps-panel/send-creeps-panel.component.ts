import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerResources, CreepDefinition, CREEP_DEFINITIONS } from '../../../../shared/models';

@Component({
  selector: 'app-send-creeps-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pane-section send-creeps-pane">
      <div class="pane-header">Send</div>
      <div class="creep-grid">
        @for (creep of creeps; track creep.id) {
          <button
            class="creep-btn"
            [class.disabled]="!canAfford(creep)"
            [disabled]="!canAfford(creep)"
            (click)="sendCreep(creep)"
            [title]="getTooltip(creep)"
          >
            <span class="creep-icon">{{ creep.icon }}</span>
            <span class="creep-name">{{ creep.name }}</span>
            <div class="cost-row">
              <span class="cost gold-cost">{{ creep.sendCost.gold }}</span>
            </div>
          </button>
        }
      </div>
    </div>
  `,
  styles: [`
    .pane-section {
      background: #0f0f23;
      border-radius: 8px;
      border: 1px solid #333;
      padding: 0.4rem;
      height: 100%;
      box-sizing: border-box;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }

    .pane-header {
      font-size: 0.7rem;
      color: #888;
      text-transform: uppercase;
      margin-bottom: 0.3rem;
      text-align: center;
    }

    .creep-grid {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      flex: 1;
    }

    .creep-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.1rem;
      padding: 0.25rem 0.2rem;
      background: #1a1a2e;
      border: 1px solid #9933ff;
      border-radius: 4px;
      color: #fff;
      cursor: pointer;
      transition: all 0.15s;
      min-width: 0;
    }

    .creep-btn:hover:not(:disabled) {
      background: #2a1a3e;
      border-color: #cc66ff;
    }

    .creep-btn:disabled,
    .creep-btn.disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .creep-icon {
      font-size: 1.1rem;
      line-height: 1;
    }

    .creep-name {
      font-size: 0.55rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100%;
    }

    .cost-row {
      display: flex;
      gap: 0.15rem;
      font-size: 0.5rem;
    }

    .cost {
      padding: 0 0.15rem;
      border-radius: 2px;
      background: rgba(0,0,0,0.3);
    }

    .gold-cost::before {
      content: '';
    }
  `]
})
export class SendCreepsPanelComponent {
  @Input() resources: PlayerResources = { wood: 100, stone: 100, gold: 100 };

  @Output() creepSent = new EventEmitter<CreepDefinition>();

  creeps = CREEP_DEFINITIONS;

  canAfford(creep: CreepDefinition): boolean {
    return this.resources.wood >= creep.sendCost.wood &&
           this.resources.stone >= creep.sendCost.stone &&
           this.resources.gold >= creep.sendCost.gold;
  }

  sendCreep(creep: CreepDefinition): void {
    if (this.canAfford(creep)) {
      this.creepSent.emit(creep);
    }
  }

  getTooltip(creep: CreepDefinition): string {
    return `Send ${creep.name}\nCost: ${creep.sendCost.gold} Gold\nSends this creep to all opponents`;
  }
}
