import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';

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

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {

  private client: Client | null = null;
  private notifications$ = new BehaviorSubject<OrderNotification | null>(null);
  private stockAlerts$ = new BehaviorSubject<StockAlert | null>(null);
  private priceUpdatesSubject$ = new BehaviorSubject<PriceUpdate[] | null>(null);


  notification$ = this.notifications$.asObservable();
  stockAlert$ = this.stockAlerts$.asObservable();
  // ── PRICE UPDATES ─────────────────────────────
  priceUpdates$ = this.priceUpdatesSubject$.asObservable();
  connect(participantId: string): void {
    if (this.client?.active) return;

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket',
      reconnectDelay: 5000,
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
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
      onStompError: (frame) => console.error('STOMP error', frame)
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}