import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService, OrderNotification, StockAlert, AdminOrderAlert, CandidatureAlert } from '../services/notification.service';
import { AuthUserSyncService } from '../../../core/auth/auth-user-sync.service';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification.component.html',
  styleUrl: './notification.component.css'
})
export class NotificationComponent implements OnInit, OnDestroy {

  // ── NOTIFICATION ARRAYS ───────────────────────
  notifications: (OrderNotification & { id: number; visible: boolean })[] = [];
  stockAlerts: (StockAlert & { id: number; visible: boolean })[] = [];
  adminAlerts: (AdminOrderAlert & { id: number; visible: boolean })[] = [];
  candidatureAlerts: (CandidatureAlert & { id: number; visible: boolean })[] = [];

  private counter = 0;

  // ── SUBSCRIPTIONS ─────────────────────────────
  private sub: Subscription | null = null;
  private stockSub: Subscription | null = null;
  private adminSub: Subscription | null = null;
  private candidatureSub: Subscription | null = null;

  constructor(
    private notificationService: NotificationService,
    private authUserSync: AuthUserSyncService
  ) {}

  ngOnInit(): void {
    // ── LISTEN FOR ORDER STATUS NOTIFICATIONS ────
    this.sub = this.notificationService.notification$.subscribe(notif => {
      if (!notif) return;
      this.showNotification(notif);
    });

    // ── LISTEN FOR STOCK ALERTS ──────────────────
    this.stockSub = this.notificationService.stockAlert$.subscribe(alert => {
      if (!alert) return;
      this.showStockAlert(alert);
    });

    // ── LISTEN FOR ADMIN ORDER ALERTS ────────────
    this.adminSub = this.notificationService.adminOrderAlert$.subscribe(alert => {
      if (!alert) return;
      this.authUserSync.currentUser$.pipe(take(1)).subscribe(user => {
        if (user?.role === 'ADMIN') {
          this.showAdminAlert(alert);
        }
      });
    });

    // ── LISTEN FOR ADMIN CANDIDATURE ALERTS ───────
    this.candidatureSub = this.notificationService.candidatureAlert$.subscribe(alert => {
      if (!alert) return;
      this.authUserSync.currentUser$.pipe(take(1)).subscribe(user => {
        if (user?.role === 'ADMIN') {
          this.showCandidatureAlert(alert);
        }
      });
    });
  }

  // ── SHOW METHODS ──────────────────────────────

  showNotification(notif: OrderNotification): void {
    const id = ++this.counter;
    this.notifications.push({ ...notif, id, visible: true });
    setTimeout(() => this.dismiss(id), 5000);
  }

  showStockAlert(alert: StockAlert): void {
    const id = ++this.counter;
    this.stockAlerts.push({ ...alert, id, visible: true });
    setTimeout(() => this.dismissStock(id), 7000);
  }

  showAdminAlert(alert: AdminOrderAlert): void {
    const id = ++this.counter;
    this.adminAlerts.push({ ...alert, id, visible: true });
    setTimeout(() => this.dismissAdmin(id), 6000);
  }

  showCandidatureAlert(alert: CandidatureAlert): void {
    const id = ++this.counter;
    this.candidatureAlerts.push({ ...alert, id, visible: true });
    setTimeout(() => this.dismissCandidature(id), 8000);
  }

  // ── DISMISS METHODS ───────────────────────────

  dismiss(id: number): void {
    const notif = this.notifications.find(n => n.id === id);
    if (notif) notif.visible = false;
    setTimeout(() => {
      this.notifications = this.notifications.filter(n => n.id !== id);
    }, 400);
  }

  dismissStock(id: number): void {
    const alert = this.stockAlerts.find(a => a.id === id);
    if (alert) alert.visible = false;
    setTimeout(() => {
      this.stockAlerts = this.stockAlerts.filter(a => a.id !== id);
    }, 400);
  }

  dismissAdmin(id: number): void {
    const alert = this.adminAlerts.find(a => a.id === id);
    if (alert) alert.visible = false;
    setTimeout(() => {
      this.adminAlerts = this.adminAlerts.filter(a => a.id !== id);
    }, 400);
  }

  dismissCandidature(id: number): void {
    const alert = this.candidatureAlerts.find(a => a.id === id);
    if (alert) alert.visible = false;
    setTimeout(() => {
      this.candidatureAlerts = this.candidatureAlerts.filter(a => a.id !== id);
    }, 400);
  }

  // ── HELPERS ───────────────────────────────────

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
    this.stockSub?.unsubscribe();
    this.adminSub?.unsubscribe();
    this.candidatureSub?.unsubscribe();
  }
}