import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TimerAudioService {
  private audioCtx: AudioContext | null = null;

  private getAudioCtx(): AudioContext {
    if (!this.audioCtx || this.audioCtx.state === 'closed') {
      this.audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    return this.audioCtx;
  }

  getTimeForDifficulty(difficulty: string, isBoss: boolean): number {
    if (isBoss) return 90;
    switch (difficulty) {
      case 'EASY':   return 60;
      case 'MEDIUM': return 45;
      case 'HARD':   return 30;
      default:       return 60;
    }
  }

  playTick(): void {
    try {
      const ctx = this.getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'square';
      osc.frequency.value = 440;
      gain.gain.setValueAtTime(0.08, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.08);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.08);
    } catch (_) {}
  }

  playUrgentTick(): void {
    try {
      const ctx = this.getAudioCtx();
      for (let i = 0; i < 2; i++) {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'square';
        osc.frequency.value = 660;
        gain.gain.setValueAtTime(0.12, ctx.currentTime + i * 0.1);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.1 + 0.06);
        osc.start(ctx.currentTime + i * 0.1);
        osc.stop(ctx.currentTime + i * 0.1 + 0.06);
      }
    } catch (_) {}
  }

  playGameOverSound(): void {
    try {
      const ctx = this.getAudioCtx();
      const notes = [523, 493, 440, 392, 349, 330, 294, 261, 196, 130];
      notes.forEach((freq, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'square';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.12, ctx.currentTime + i * 0.08);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.08 + 0.07);
        osc.start(ctx.currentTime + i * 0.08);
        osc.stop(ctx.currentTime + i * 0.08 + 0.07);
      });
    } catch (_) {}
  }

  playSuccessSound(): void {
    try {
      const ctx = this.getAudioCtx();
      const notes = [262, 330, 392, 523, 659, 784];
      notes.forEach((freq, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'square';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.1, ctx.currentTime + i * 0.07);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.07 + 0.06);
        osc.start(ctx.currentTime + i * 0.07);
        osc.stop(ctx.currentTime + i * 0.07 + 0.06);
      });
    } catch (_) {}
  }

  playErrorSound(): void {
    try {
      const ctx = this.getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'square';
      osc.frequency.setValueAtTime(200, ctx.currentTime);
      osc.frequency.setValueAtTime(150, ctx.currentTime + 0.1);
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.25);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.25);
    } catch (_) {}
  }

  /** Very quiet tick played every second during normal countdown (>15s). */
  playBaseTick(): void {
    try {
      const ctx = this.getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'sine';
      osc.frequency.value = 320;
      gain.gain.setValueAtTime(0.03, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.05);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.05);
    } catch (_) {}
  }

  /** Soft high-pitched blip for button hover. */
  playHover(): void {
    try {
      const ctx = this.getAudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'sine';
      osc.frequency.setValueAtTime(900, ctx.currentTime);
      osc.frequency.linearRampToValueAtTime(1100, ctx.currentTime + 0.04);
      gain.gain.setValueAtTime(0.04, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.06);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.06);
    } catch (_) {}
  }

  /** Smooth cinematic entry sound for starting / retrying a mission. */
  playMissionEntry(): void {
    try {
      const ctx = this.getAudioCtx();
      const t = ctx.currentTime;

      // Layer 1 — low bass rumble, fades in then out
      const bass = ctx.createOscillator();
      const bassGain = ctx.createGain();
      bass.connect(bassGain);
      bassGain.connect(ctx.destination);
      bass.type = 'sine';
      bass.frequency.setValueAtTime(55, t);
      bass.frequency.linearRampToValueAtTime(85, t + 0.9);
      bassGain.gain.setValueAtTime(0, t);
      bassGain.gain.linearRampToValueAtTime(0.09, t + 0.25);
      bassGain.gain.linearRampToValueAtTime(0.05, t + 0.9);
      bassGain.gain.exponentialRampToValueAtTime(0.001, t + 1.3);
      bass.start(t);
      bass.stop(t + 1.3);

      // Layer 2 — mid rising sweep
      const sweep = ctx.createOscillator();
      const sweepGain = ctx.createGain();
      sweep.connect(sweepGain);
      sweepGain.connect(ctx.destination);
      sweep.type = 'sine';
      sweep.frequency.setValueAtTime(180, t + 0.1);
      sweep.frequency.exponentialRampToValueAtTime(900, t + 0.75);
      sweepGain.gain.setValueAtTime(0, t + 0.1);
      sweepGain.gain.linearRampToValueAtTime(0.07, t + 0.3);
      sweepGain.gain.exponentialRampToValueAtTime(0.001, t + 1.0);
      sweep.start(t + 0.1);
      sweep.stop(t + 1.0);

      // Layer 3 — high accent chord (two notes) at the peak
      [1200, 1500].forEach((freq, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, t + 0.65 + i * 0.06);
        osc.frequency.linearRampToValueAtTime(freq * 1.08, t + 1.3);
        gain.gain.setValueAtTime(0, t + 0.65 + i * 0.06);
        gain.gain.linearRampToValueAtTime(0.05, t + 0.75 + i * 0.06);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 1.4);
        osc.start(t + 0.65 + i * 0.06);
        osc.stop(t + 1.4);
      });

      // Layer 4 — subtle white-noise whoosh
      const bufferSize = ctx.sampleRate * 0.8;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
      const noise = ctx.createBufferSource();
      noise.buffer = buffer;
      const noiseFilter = ctx.createBiquadFilter();
      noiseFilter.type = 'bandpass';
      noiseFilter.frequency.setValueAtTime(600, t + 0.1);
      noiseFilter.frequency.linearRampToValueAtTime(2400, t + 0.7);
      noiseFilter.Q.value = 0.8;
      const noiseGain = ctx.createGain();
      noiseGain.gain.setValueAtTime(0, t + 0.1);
      noiseGain.gain.linearRampToValueAtTime(0.04, t + 0.35);
      noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.85);
      noise.connect(noiseFilter);
      noiseFilter.connect(noiseGain);
      noiseGain.connect(ctx.destination);
      noise.start(t + 0.1);
      noise.stop(t + 0.85);

    } catch (_) {}
  }

  /** Satisfying confirm beep for button click. */
  playClick(): void {
    try {
      const ctx = this.getAudioCtx();
      const t = ctx.currentTime;
      const notes = [600, 800];
      notes.forEach((freq, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'square';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.07, t + i * 0.05);
        gain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.05 + 0.06);
        osc.start(t + i * 0.05);
        osc.stop(t + i * 0.05 + 0.06);
      });
    } catch (_) {}
  }
}
