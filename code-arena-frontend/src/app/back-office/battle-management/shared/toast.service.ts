import { Injectable, signal } from '@angular/core';

export type ToastTone = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: number;
  tone: ToastTone;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<ToastMessage[]>([]);

  success(text: string) {
    this.push('success', text);
  }

  error(text: string) {
    this.push('error', text);
  }

  info(text: string) {
    this.push('info', text);
  }

  dismiss(id: number) {
    this.toasts.update((all) => all.filter((t) => t.id !== id));
  }

  private push(tone: ToastTone, text: string) {
    const id = this.nextId++;
    this.toasts.update((all) => [...all, { id, tone, text }]);
    setTimeout(() => this.dismiss(id), 4500);
  }
}
