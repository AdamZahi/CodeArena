import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService, OrderNotification } from '../services/notification.service';import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification.component.html',
  styleUrl: './notification.component.css'
})
export class NotificationComponent implements OnInit, OnDestroy {

  notifications: (OrderNotification & { id: number; visible: boolean })[] = [];
  private counter = 0;
  private sub: Subscription | null = null;

constructor(
  private notificationService: NotificationService,
  private auth: AuthService
) {}

  ngOnInit(): void {
    // Connect WebSocket with real user sub
    this.auth.user$.pipe(take(1)).subscribe(user => {
      const participantId = user?.sub;
      if (participantId) {
        this.notificationService.connect(participantId);
      }
    });

    // Listen for incoming notifications
    this.sub = this.notificationService.notification$.subscribe(notif => {
      if (!notif) return;
      this.showNotification(notif);
    });
  }

  showNotification(notif: OrderNotification): void {
    const id = ++this.counter;
    this.notifications.push({ ...notif, id, visible: true });

    // Auto-remove after 5 seconds
    setTimeout(() => this.dismiss(id), 5000);
  }

  dismiss(id: number): void {
    const notif = this.notifications.find(n => n.id === id);
    if (notif) notif.visible = false;
    setTimeout(() => {
      this.notifications = this.notifications.filter(n => n.id !== id);
    }, 400);
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'CONFIRMED': return '🎉';
      case 'SHIPPED':   return '🚚';
      case 'DELIVERED': return '✅';
      case 'CANCELLED': return '❌';
      default:          return '📦';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'notif-confirmed';
      case 'SHIPPED':   return 'notif-shipped';
      case 'DELIVERED': return 'notif-delivered';
      case 'CANCELLED': return 'notif-cancelled';
      default:          return 'notif-default';
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.notificationService.disconnect();
  }
}