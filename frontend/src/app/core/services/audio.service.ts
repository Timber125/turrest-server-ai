import { Injectable } from '@angular/core';

/**
 * AudioService for playing game sound effects.
 * Supports loading and playing sounds for resource events.
 */
@Injectable({ providedIn: 'root' })
export class AudioService {
  private audioContext: AudioContext | null = null;
  private sounds: Map<string, AudioBuffer> = new Map();
  private volume = 0.5;
  private muted = false;
  private loaded = false;

  // Sound mappings for different event types
  private readonly SOUND_MAP: Record<string, string> = {
    kill: 'coin',
    hit: 'coin',
    build: 'build',
    tower: 'tower',
    send: 'send'
  };

  constructor() {
    // Initialize audio context on first user interaction
    if (typeof window !== 'undefined') {
      const initAudio = () => {
        if (!this.audioContext) {
          this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
          this.loadSounds();
        }
        window.removeEventListener('click', initAudio);
        window.removeEventListener('keydown', initAudio);
      };
      window.addEventListener('click', initAudio);
      window.addEventListener('keydown', initAudio);
    }
  }

  /**
   * Load all game sound effects.
   */
  async loadSounds(): Promise<void> {
    if (this.loaded || !this.audioContext) return;

    const soundFiles = [
      { id: 'coin', path: '/assets/audio/coin.mp3' },
      { id: 'build', path: '/assets/audio/build.mp3' },
      { id: 'tower', path: '/assets/audio/tower.mp3' },
      { id: 'send', path: '/assets/audio/send.mp3' }
    ];

    for (const sound of soundFiles) {
      try {
        const response = await fetch(sound.path);
        if (response.ok) {
          const arrayBuffer = await response.arrayBuffer();
          const audioBuffer = await this.audioContext.decodeAudioData(arrayBuffer);
          this.sounds.set(sound.id, audioBuffer);
          console.log(`Loaded sound: ${sound.id}`);
        }
      } catch (error) {
        console.warn(`Failed to load sound: ${sound.id}`, error);
      }
    }

    this.loaded = true;
  }

  /**
   * Play a sound effect by ID.
   * @param soundId The ID of the sound to play (coin, build, tower, send)
   * @param volumeMultiplier Optional volume multiplier (0-1)
   */
  play(soundId: string, volumeMultiplier = 1): void {
    if (this.muted || !this.audioContext) return;

    const buffer = this.sounds.get(soundId);
    if (!buffer) {
      console.warn(`Sound not found: ${soundId}`);
      return;
    }

    try {
      const source = this.audioContext.createBufferSource();
      source.buffer = buffer;

      const gainNode = this.audioContext.createGain();
      gainNode.gain.value = this.volume * volumeMultiplier;

      source.connect(gainNode);
      gainNode.connect(this.audioContext.destination);

      source.start(0);
    } catch (error) {
      console.warn(`Failed to play sound: ${soundId}`, error);
    }
  }

  /**
   * Play sound for a resource event type.
   * @param eventType The event type (kill, hit, build, tower, send)
   */
  playForEvent(eventType: string): void {
    const soundId = this.SOUND_MAP[eventType];
    if (soundId) {
      this.play(soundId);
    }
  }

  /**
   * Set the master volume.
   * @param volume Volume level (0-1)
   */
  setVolume(volume: number): void {
    this.volume = Math.max(0, Math.min(1, volume));
  }

  /**
   * Get the current volume level.
   */
  getVolume(): number {
    return this.volume;
  }

  /**
   * Mute all sounds.
   */
  mute(): void {
    this.muted = true;
  }

  /**
   * Unmute sounds.
   */
  unmute(): void {
    this.muted = false;
  }

  /**
   * Toggle mute state.
   */
  toggleMute(): boolean {
    this.muted = !this.muted;
    return this.muted;
  }

  /**
   * Check if audio is muted.
   */
  isMuted(): boolean {
    return this.muted;
  }
}
