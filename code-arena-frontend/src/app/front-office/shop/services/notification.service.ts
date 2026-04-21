import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';

export interface OrderNotification {
  orderId: string;
  status: string;
  message: string;
}

export interface StockAlert {
  productId: string;
  productName: string;
  stock: number;
  message: string;
}

export interface PriceUpdate {
  productId: string;
  originalPrice: number;
  dynamicPrice: number;
  indicator: string;
  changed: boolean;
}

export interface AdminOrderAlert {
  orderId: string;
  participantId: string;
  total: number;
  message: string;
}

export interface CandidatureAlert {
  type: string;
  eventId: string;
  eventTitle: string;
  participantId: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {

  private client: Client | null = null;
  private notifications$       = new BehaviorSubject<OrderNotification | null>(null);
  private stockAlerts$         = new BehaviorSubject<StockAlert | null>(null);
  private priceUpdatesSubject$ = new BehaviorSubject<PriceUpdate[] | null>(null);
  private adminOrderAlerts$    = new BehaviorSubject<AdminOrderAlert | null>(null);
  private candidatureAlerts$   = new BehaviorSubject<CandidatureAlert | null>(null);
  private loyaltyMilestone$    = new BehaviorSubject<any | null>(null);

  adminOrderAlert$  = this.adminOrderAlerts$.asObservable();
  notification$     = this.notifications$.asObservable();
  stockAlert$       = this.stockAlerts$.asObservable();
  candidatureAlert$ = this.candidatureAlerts$.asObservable();
  priceUpdates$     = this.priceUpdatesSubject$.asObservable();
  milestone$        = this.loyaltyMilestone$.asObservable();

  constructor(private auth: AuthService) {}

  connect(participantId: string): void {
    if (this.client?.active) return;

    // Get JWT token from Auth0 and pass it to STOMP
    this.auth.getAccessTokenSilently().pipe(take(1)).subscribe({
      next: (token) => {
        this.client = new Client({
          brokerURL: 'ws://localhost:8080/ws/websocket',
          reconnectDelay: 5000,
          connectHeaders: {
            Authorization: `Bearer ${token}`
          },
          onConnect: () => {
            console.log('WebSocket connected');

            // ── ORDER NOTIFICATIONS ──────────────────
            this.client?.subscribe(
              `/topic/orders/${participantId}`,
              (message: IMessage) => {
                const notification: OrderNotification = JSON.parse(message.body);
                this.notifications$.next(notification);
              }
            );

            // ── STOCK ALERTS ─────────────────────────
            this.client?.subscribe(
              `/topic/stock-alert`,
              (message: IMessage) => {
                const alert: StockAlert = JSON.parse(message.body);
                this.stockAlerts$.next(alert);
              }
            );

            // ── PRICE UPDATES ─────────────────────────
            this.client?.subscribe(
              `/topic/price-updates`,
              (message: IMessage) => {
                const updates: PriceUpdate[] = JSON.parse(message.body);
                this.priceUpdatesSubject$.next(updates);
              }
            );

            // ── ADMIN ORDER ALERTS ────────────────────
            this.client?.subscribe(
              `/topic/admin/new-order`,
              (message: IMessage) => {
                const alert: AdminOrderAlert = JSON.parse(message.body);
                this.adminOrderAlerts$.next(alert);
              }
            );

            // ── ADMIN CANDIDATURE ALERTS (Nesrine) ────
            this.client?.subscribe(
              `/topic/admin/candidatures`,
              (message: IMessage) => {
                const alert: CandidatureAlert = JSON.parse(message.body);
                this.candidatureAlerts$.next(alert);
              }
            );

            // ── LOYALTY MILESTONE NOTIFICATIONS ───────
            this.client?.subscribe(
              `/topic/loyalty/${participantId}`,
              (message: IMessage) => {
                const milestone = JSON.parse(message.body);
                this.loyaltyMilestone$.next(milestone);
              }
            );
          },
          onDisconnect: () => console.log('WebSocket disconnected'),
          onStompError: (frame) => console.error('STOMP error', frame)
        });

        this.client.activate();
      },
      error: () => {
        // Token fetch failed — connect without token (dev fallback)
        this.client = new Client({
          brokerURL: 'ws://localhost:8080/ws/websocket',
          reconnectDelay: 5000,
          onConnect: () => {
            console.log('WebSocket connected (no auth)');
            this.client?.subscribe(`/topic/orders/${participantId}`, (message: IMessage) => {
              this.notifications$.next(JSON.parse(message.body));
            });
            this.client?.subscribe(`/topic/stock-alert`, (message: IMessage) => {
              this.stockAlerts$.next(JSON.parse(message.body));
            });
            this.client?.subscribe(`/topic/price-updates`, (message: IMessage) => {
              this.priceUpdatesSubject$.next(JSON.parse(message.body));
            });
            this.client?.subscribe(`/topic/admin/new-order`, (message: IMessage) => {
              this.adminOrderAlerts$.next(JSON.parse(message.body));
            });
            this.client?.subscribe(`/topic/admin/candidatures`, (message: IMessage) => {
              this.candidatureAlerts$.next(JSON.parse(message.body));
            });
            this.client?.subscribe(`/topic/loyalty/${participantId}`, (message: IMessage) => {
              this.loyaltyMilestone$.next(JSON.parse(message.body));
            });
          },
          onDisconnect: () => console.log('WebSocket disconnected'),
          onStompError: (frame) => console.error('STOMP error', frame)
        });
        this.client.activate();
      }
    });
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}