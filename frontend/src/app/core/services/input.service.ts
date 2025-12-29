import { Injectable, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';
import {
  InputAction,
  InputContext,
  KeyBinding,
  INPUT_ACTION_REGISTRY,
  getDefaultBinding
} from '../../shared/models';
import { SettingsService } from './settings.service';

@Injectable({ providedIn: 'root' })
export class InputService {
  private readonly settingsService = inject(SettingsService);

  // Current input context stack (first = highest priority)
  private contextStack = signal<InputContext[]>([InputContext.GAME]);

  // Action stream for subscribers
  private actionSubject = new Subject<InputAction>();
  readonly action$ = this.actionSubject.asObservable();

  // Rebinding mode state
  private rebindingAction = signal<InputAction | null>(null);
  private rebindCallback: ((binding: KeyBinding) => void) | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('keydown', (e) => this.handleKeyDown(e));
    }
  }

  /**
   * Get current active context (highest priority).
   */
  get currentContext(): InputContext {
    return this.contextStack()[0] ?? InputContext.GAME;
  }

  /**
   * Check if currently in rebinding mode.
   */
  get isRebinding(): boolean {
    return this.rebindingAction() !== null;
  }

  /**
   * Get the action currently being rebound.
   */
  get rebindingActionValue(): InputAction | null {
    return this.rebindingAction();
  }

  /**
   * Push a context onto the stack (e.g., when opening menu).
   */
  pushContext(context: InputContext): void {
    this.contextStack.update(stack => {
      // Don't add duplicate context
      if (stack[0] === context) return stack;
      return [context, ...stack.filter(c => c !== context)];
    });
  }

  /**
   * Remove a context from the stack (e.g., when closing menu).
   */
  popContext(context: InputContext): void {
    this.contextStack.update(stack => stack.filter(c => c !== context));
  }

  /**
   * Set context directly (replaces stack).
   */
  setContext(context: InputContext): void {
    this.contextStack.set([context]);
  }

  /**
   * Check if a specific context is active (anywhere in stack).
   */
  hasContext(context: InputContext): boolean {
    return this.contextStack().includes(context);
  }

  /**
   * Start listening for next key to rebind an action.
   * Returns a promise that resolves with the captured binding.
   */
  startRebinding(action: InputAction): Promise<KeyBinding> {
    return new Promise((resolve) => {
      this.rebindingAction.set(action);
      this.rebindCallback = resolve;
    });
  }

  /**
   * Cancel rebinding mode.
   */
  cancelRebinding(): void {
    this.rebindingAction.set(null);
    this.rebindCallback = null;
  }

  private handleKeyDown(event: KeyboardEvent): void {
    // Skip if in text input
    if (this.isTextInput(event.target)) {
      return;
    }

    // Handle rebinding mode - capture the key
    if (this.rebindingAction() !== null) {
      event.preventDefault();
      event.stopPropagation();

      const binding: KeyBinding = {
        key: event.key,
        ctrl: event.ctrlKey || undefined,
        shift: event.shiftKey || undefined,
        alt: event.altKey || undefined,
      };

      // Clean up undefined modifiers
      if (!binding.ctrl) delete binding.ctrl;
      if (!binding.shift) delete binding.shift;
      if (!binding.alt) delete binding.alt;

      this.rebindCallback?.(binding);
      this.cancelRebinding();
      return;
    }

    // Find matching action for current context
    const currentContext = this.currentContext;
    const customBindings = this.settingsService.getKeyBindings();

    // Check actions in registry order (first match wins)
    for (const meta of INPUT_ACTION_REGISTRY) {
      // Check if action is valid in current context
      if (!meta.contexts.includes(currentContext)) {
        continue;
      }

      // Get custom binding or default
      const binding = customBindings.get(meta.action) ?? meta.defaultBinding;

      if (this.matchesBinding(event, binding)) {
        event.preventDefault();
        this.actionSubject.next(meta.action);
        return;  // First match wins
      }
    }
  }

  private matchesBinding(event: KeyboardEvent, binding: KeyBinding): boolean {
    // Key must match (case-insensitive for letters)
    const keyMatches = event.key.toLowerCase() === binding.key.toLowerCase();
    if (!keyMatches) return false;

    // Modifier keys must match exactly when specified
    const ctrlMatches = binding.ctrl ? event.ctrlKey : !event.ctrlKey || binding.ctrl === undefined;
    const shiftMatches = binding.shift ? event.shiftKey : !event.shiftKey || binding.shift === undefined;
    const altMatches = binding.alt ? event.altKey : !event.altKey || binding.alt === undefined;

    // If modifier is specified in binding, it must be pressed
    // If modifier is not specified, it should not be pressed (unless undefined means "don't care")
    // For simplicity: if binding.ctrl is true, event.ctrlKey must be true
    //                 if binding.ctrl is undefined, we don't care about ctrl state
    const ctrlOk = binding.ctrl === undefined ? true : (binding.ctrl === event.ctrlKey);
    const shiftOk = binding.shift === undefined ? true : (binding.shift === event.shiftKey);
    const altOk = binding.alt === undefined ? true : (binding.alt === event.altKey);

    return ctrlOk && shiftOk && altOk;
  }

  private isTextInput(target: EventTarget | null): boolean {
    if (!target) return false;
    const element = target as HTMLElement;
    return (
      element instanceof HTMLInputElement ||
      element instanceof HTMLTextAreaElement ||
      element.isContentEditable
    );
  }
}
