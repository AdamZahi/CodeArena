import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService, OrderNotification, StockAlert, AdminOrderAlert } from '../services/notification.service';
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

  // ── ORDER NOTIFICATIONS ───────────────────────
  // Shows when admin updates order status (CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
  // Only the participant whose order changed sees this
  notifications: (OrderNotification & { id: number; visible: boolean })[] = [];

  // ── STOCK ALERTS ──────────────────────────────
  // Shows when any product stock drops to ≤5 after a purchase
  // Visible to ALL users browsing the shop (broadcast to everyone)
  stockAlerts: (StockAlert & { id: number; visible: boolean })[] = [];

  // ── ADMIN ORDER ALERTS ────────────────────────
  // Shows when a new order is placed by any participant
  // Only visible to ADMIN users (checked via role)
  adminAlerts: (AdminOrderAlert & { id: number; visible: boolean })[] = [];

  // ── COUNTER ───────────────────────────────────
  // Used to give each notification a unique ID so we can dismiss them individually
  private counter = 0;

  // ── SUBSCRIPTIONS ─────────────────────────────
  // We store subscriptions so we can unsubscribe on destroy (prevent memory leaks)
  private sub: Subscription | null = null;
  private stockSub: Subscription | null = null;
  private adminSub: Subscription | null = null;

  constructor(
    // NotificationService manages the WebSocket connection and exposes observables
    private notificationService: NotificationService,
    // AuthUserSyncService gives us the current logged-in user with their role
    private authUserSync: AuthUserSyncService
    // NOTE: AuthService removed — WebSocket connection is now handled in app.component.ts
    // This prevents the connect/disconnect conflict that was causing notifications to fail
  ) {}

  ngOnInit(): void {
    // ── NOTE: WebSocket connection is handled in app.component.ts ─────────────
    // We do NOT call notificationService.connect() here anymore
    // app.component.ts connects once when the app loads and stays connected
    // This prevents the WebSocket from disconnecting when this component re-renders

    // ── LISTEN FOR ORDER STATUS NOTIFICATIONS ────
    // Fires when admin changes order status (confirm/ship/deliver/cancel)
    // Backend sends to /topic/orders/{participantId} — only that participant sees it
    this.sub = this.notificationService.notification$.subscribe(notif => {
      if (!notif) return;
      this.showNotification(notif);
    });

    // ── LISTEN FOR STOCK ALERTS ──────────────────
    // Fires when stock drops to ≤5 after any purchase
    // Backend sends to /topic/stock-alert — ALL users see this
    this.stockSub = this.notificationService.stockAlert$.subscribe(alert => {
      if (!alert) return;
      this.showStockAlert(alert);
    });

    // ── LISTEN FOR ADMIN ORDER ALERTS ────────────
    // Fires when any participant places a new order
    // Backend sends to /topic/admin/new-order — but we check role here
    // Only users with role === 'ADMIN' will see this toast
    this.adminSub = this.notificationService.adminOrderAlert$.subscribe(alert => {
      if (!alert) return;
      // Check current user role before showing the alert
      this.authUserSync.currentUser$.pipe(take(1)).subscribe(user => {
        if (user?.role === 'ADMIN') {
          this.showAdminAlert(alert);
        }
        // If user is not ADMIN, we silently ignore the alert
      });
    });
  }

  // ── ORDER NOTIFICATION METHODS ────────────────

  // Creates a new order notification toast and auto-removes after 5 seconds
  showNotification(notif: OrderNotification): void {
    const id = ++this.counter;
    this.notifications.push({ ...notif, id, visible: true });
    // Auto-remove after 5 seconds
    setTimeout(() => this.dismiss(id), 5000);
  }

  // Hides the toast with animation then removes it from the array
  dismiss(id: number): void {
    const notif = this.notifications.find(n => n.id === id);
    if (notif) notif.visible = false; // triggers CSS fade-out animation
    setTimeout(() => {
      this.notifications = this.notifications.filter(n => n.id !== id);
    }, 400); // wait for animation to finish before removing from DOM
  }

  // ── STOCK ALERT METHODS ───────────────────────

  // Creates a new stock alert toast and auto-removes after 7 seconds
  // 7 seconds (longer than order notif) because stock info is more urgent
  showStockAlert(alert: StockAlert): void {
    const id = ++this.counter;
    this.stockAlerts.push({ ...alert, id, visible: true });
    setTimeout(() => this.dismissStock(id), 7000);
  }

  dismissStock(id: number): void {
    const alert = this.stockAlerts.find(a => a.id === id);
    if (alert) alert.visible = false;
    setTimeout(() => {
      this.stockAlerts = this.stockAlerts.filter(a => a.id !== id);
    }, 400);
  }

  // ── ADMIN ORDER ALERT METHODS ─────────────────

  // Creates a new admin alert toast and auto-removes after 6 seconds
  showAdminAlert(alert: AdminOrderAlert): void {
    const id = ++this.counter;
    this.adminAlerts.push({ ...alert, id, visible: true });
    setTimeout(() => this.dismissAdmin(id), 6000);
  }

  dismissAdmin(id: number): void {
    const alert = this.adminAlerts.find(a => a.id === id);
    if (alert) alert.visible = false;
    setTimeout(() => {
      this.adminAlerts = this.adminAlerts.filter(a => a.id !== id);
    }, 400);
  }

  // ── STATUS ICON HELPER ────────────────────────
  // Returns an emoji based on order status for the notification toast
  getStatusIcon(status: string): string {
    switch (status) {
      case 'CONFIRMED': return '🎉';
      case 'SHIPPED':   return '🚚';
      case 'DELIVERED': return '✅';
      case 'CANCELLED': return '❌';
      default:          return '📦';
    }
  }

  // ── STATUS COLOR HELPER ───────────────────────
  // Returns a CSS class based on order status for colored border on toast
  getStatusColor(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'notif-confirmed'; // purple
      case 'SHIPPED':   return 'notif-shipped';   // cyan
      case 'DELIVERED': return 'notif-delivered'; // green
      case 'CANCELLED': return 'notif-cancelled'; // red
      default:          return 'notif-default';
    }
  }

  ngOnDestroy(): void {
    // ── CLEAN UP SUBSCRIPTIONS ────────────────────
    // Always unsubscribe to prevent memory leaks when component is destroyed
    this.sub?.unsubscribe();
    this.stockSub?.unsubscribe();
    this.adminSub?.unsubscribe();

    // ── NOTE: We do NOT disconnect WebSocket here ─
    // The WebSocket is managed by app.component.ts and should stay alive
    // for the entire app session, not just when this component exists
  }
}