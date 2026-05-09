import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TtsService {

  private audioCtx: AudioContext | null = null;
  private currentSource: AudioBufferSourceNode | null = null;
  


  isMuted = false;
  isPlaying = false;

  constructor(private http: HttpClient) {}

  private getAudioCtx(): AudioContext {
    if (!this.audioCtx || this.audioCtx.state === 'closed') {
      this.audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    return this.audioCtx;
  }

  speak(text: string, voice = 'Charon', style = ''): void {
    if (this.isMuted) return;

    this.http.post(`${environment.apiBaseUrl}/api/terminal-quest/tts`,
      { text, voiceName: voice, style },
      { responseType: 'arraybuffer' }
    ).subscribe({
      next: (buffer) => {
        if (this.isMuted) return;
        const ctx = this.getAudioCtx();
        ctx.decodeAudioData(buffer).then(audioBuffer => {
          this.stop();
          const source = ctx.createBufferSource();
          source.buffer = audioBuffer;
          source.connect(ctx.destination);
          source.onended = () => { this.isPlaying = false; };
          this.currentSource = source;
          this.isPlaying = true;
          source.start(0);
        }).catch(() => {});
      },
      error: () => {}
    });
  }

  toggleMute(): void {
    this.isMuted = !this.isMuted;
    if (this.isMuted) this.stop();
  }

  stop(): void {
    if (this.currentSource) {
      try { this.currentSource.stop(); } catch (_) {}
      this.currentSource = null;
    }
    this.isPlaying = false;
  }
}
