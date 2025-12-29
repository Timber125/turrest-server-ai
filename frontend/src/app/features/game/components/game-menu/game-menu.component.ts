import { Component, OnInit, OnDestroy, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { InputService, SettingsService } from '../../../../core/services';
import { InputAction, InputContext } from '../../../../shared/models';
import { OptionsPanelComponent } from './options-panel.component';
import { StatsPanelComponent } from './stats-panel.component';

@Component({
  selector: 'app-game-menu',
  standalone: true,
  imports: [CommonModule, OptionsPanelComponent, StatsPanelComponent],
  template: `
    <div class="menu-overlay" (click)="onOverlayClick($event)">
      <div class="menu-panel" (click)="$event.stopPropagation()">
        @switch (activeTab()) {
          @case ('main') {
            <h2>Menu</h2>
            <div class="menu-buttons">
              <button class="menu-btn" (click)="onContinue()">
                <span class="btn-icon">▶</span>
                Continue
              </button>
              <button class="menu-btn" (click)="activeTab.set('options')">
                <span class="btn-icon">⚙</span>
                Options
              </button>
              <button class="menu-btn" (click)="activeTab.set('stats')">
                <span class="btn-icon">📊</span>
                Stats
              </button>
              <button class="menu-btn quit-btn" (click)="onQuit()">
                <span class="btn-icon">✖</span>
                Quit Game
              </button>
            </div>
            <p class="hint">Press Escape to close</p>
          }
          @case ('options') {
            <app-options-panel (back)="activeTab.set('main')"></app-options-panel>
          }
          @case ('stats') {
            <app-stats-panel (back)="activeTab.set('main')"></app-stats-panel>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .menu-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.75);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.15s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .menu-panel {
      background: #1a1a2e;
      border: 2px solid #00d9ff;
      border-radius: 12px;
      padding: 1.5rem 2rem;
      min-width: 320px;
      max-width: 500px;
      max-height: 80vh;
      overflow-y: auto;
      box-shadow: 0 0 30px rgba(0, 217, 255, 0.3);
      animation: slideIn 0.2s ease-out;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: scale(0.95) translateY(-10px);
      }
      to {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
    }

    h2 {
      color: #00d9ff;
      margin: 0 0 1.5rem;
      text-align: center;
      font-size: 1.5rem;
    }

    .menu-buttons {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .menu-btn {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1.25rem;
      background: #0f0f23;
      border: 1px solid #333;
      color: #fff;
      font-size: 1rem;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .menu-btn:hover {
      background: #1f1f3f;
      border-color: #00d9ff;
      transform: translateX(4px);
    }

    .menu-btn:active {
      transform: translateX(2px);
    }

    .btn-icon {
      font-size: 1.1rem;
      width: 24px;
      text-align: center;
    }

    .quit-btn {
      margin-top: 0.5rem;
      border-color: #ff4444;
    }

    .quit-btn:hover {
      background: rgba(255, 68, 68, 0.2);
      border-color: #ff6666;
    }

    .hint {
      color: #666;
      font-size: 0.8rem;
      text-align: center;
      margin: 1rem 0 0;
    }
  `]
})
export class GameMenuComponent implements OnInit, OnDestroy {
  @Output() continue = new EventEmitter<void>();
  @Output() quit = new EventEmitter<void>();

  private readonly inputService = inject(InputService);
  private actionSub: Subscription | null = null;

  activeTab = signal<'main' | 'options' | 'stats'>('main');

  ngOnInit(): void {
    // Push modal context to block game inputs
    this.inputService.pushContext(InputContext.MODAL);

    // Listen for menu toggle action to close
    this.actionSub = this.inputService.action$.subscribe(action => {
      if (action === InputAction.MENU_TOGGLE) {
        this.onContinue();
      }
    });
  }

  ngOnDestroy(): void {
    this.inputService.popContext(InputContext.MODAL);
    this.actionSub?.unsubscribe();
  }

  onOverlayClick(event: MouseEvent): void {
    this.onContinue();
  }

  onContinue(): void {
    this.continue.emit();
  }

  onQuit(): void {
    this.quit.emit();
  }
}
