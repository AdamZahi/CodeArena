import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {

  private toasts = new BehaviorSubject<Toast[]>([]);
  toasts$ = this.toasts.asObservable();
  private counter = 0;

  show(message: string, type: Toast['type'] = 'info', duration = 3000): void {
    const id = ++this.counter;
    const toast: Toast = { id, message, type, duration };
    this.toasts.next([...this.toasts.getValue(), toast]);
    setTimeout(() => this.remove(id), duration);
  }

  success(message: string): void { this.show(message, 'success'); }
  error(message: string):   void { this.show(message, 'error', 4000); }
  warning(message: string): void { this.show(message, 'warning'); }
  info(message: string):    void { this.show(message, 'info'); }

  remove(id: number): void {
    this.toasts.next(this.toasts.getValue().filter(t => t.id !== id));
  }
}