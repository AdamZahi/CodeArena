import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface AlertData {
  title: string;
  message: string;
  type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';
  isConfirm?: boolean;
  confirmText?: string;
  cancelText?: string;
  resolve?: (value: boolean) => void;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alertSubject = new Subject<AlertData>();
  alertState$ = this.alertSubject.asObservable();

  constructor() {}

  showAlert(title: string, message: string, type: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR' = 'INFO'): void {
    this.alertSubject.next({ title, message, type, isConfirm: false });
  }

  showConfirm(title: string, message: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.alertSubject.next({
        title,
        message,
        type: 'WARNING',
        isConfirm: true,
        confirmText: 'CONFIRM',
        cancelText: 'CANCEL',
        resolve
      });
    });
  }

  success(message: string, title: string = 'SUCCESS'): void {
    this.showAlert(title, message, 'SUCCESS');
  }

  error(message: string, title: string = 'ERROR'): void {
    this.showAlert(title, message, 'ERROR');
  }

  info(message: string, title: string = 'INFO'): void {
    this.showAlert(title, message, 'INFO');
  }

  warning(message: string, title: string = 'WARNING'): void {
    this.showAlert(title, message, 'WARNING');
  }
}
