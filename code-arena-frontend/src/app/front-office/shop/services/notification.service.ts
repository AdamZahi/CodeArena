import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';

export interface OrderNotification {
  orderId: string;
  status: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {

  private client: Client | null = null;
  private notifications$ = new BehaviorSubject<OrderNotification | null>(null);

  notification$ = this.notifications$.asObservable();

  connect(participantId: string): void {
    if (this.client?.active) return;

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket',
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected');
        this.client?.subscribe(
          `/topic/orders/${participantId}`,
          (message: IMessage) => {
            const notification: OrderNotification = JSON.parse(message.body);
            this.notifications$.next(notification);
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