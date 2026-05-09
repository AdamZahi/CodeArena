import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import { AuthService } from '@auth0/auth0-angular';
import { firstValueFrom } from 'rxjs';

// ── INTERFACES ────────────────────────────────────────────────────────────────
// Each interface matches exactly what the backend sends via WebSocket
// These are typed so Angular knows what to expect in each topic

// Sent by PurchaseServiceImpl.updateOrderStatus()
// Topic: /topic/orders/{participantId}
export interface OrderNotification {
  orderId: string;
  status: string;
  message: string;
}

// Sent by PurchaseServiceImpl.checkout() when stock drops to ≤ 5
// Topic: /topic/stock-alert
export interface StockAlert {
  productId: string;
  productName: string;
  stock: number;
  message: string;
}

// Sent by DynamicPricingService every 30 seconds
// Topic: /topic/price-updates
export interface PriceUpdate {
  productId: string;
  originalPrice: number;
  dynamicPrice: number;
  indicator: string;  // "🔥 HOT", "📈 SURGE", "💰 DEAL", etc.
  changed: boolean;   // true = price differs from original
}

// Sent by PurchaseServiceImpl.checkout() to notify admin of new order
// Topic: /topic/admin/new-order
export interface AdminOrderAlert {
  orderId: string;
  participantId: string;
  total: number;
  message: string;
}

// Sent by event module (Nesrine) when candidature status changes
// Topic: /topic/admin/candidatures
export interface CandidatureAlert {
  type: string;
  eventId: string;
  eventTitle: string;
  participantId: string;
  message: string;
}

// ── SERVICE ───────────────────────────────────────────────────────────────────
// NotificationService manages the single WebSocket connection for the entire app
// Uses STOMP protocol over raw WebSocket — same pattern as battle-websocket.service.ts
// All components subscribe to observables — they never touch the WebSocket directly
@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {

  // ── STOMP CLIENT ──────────────────────────────────────────────────────────
  // Single WebSocket client for the whole app
  // null when disconnected, active when connected
  private client: Client | null = null;

  // ── BEHAVIORSUBJECTS ──────────────────────────────────────────────────────
  // BehaviorSubject holds the LAST emitted value
  // New subscribers immediately get the current value
  // null = no notification received yet
  private notifications$       = new BehaviorSubject<OrderNotification | null>(null);
  private stockAlerts$         = new BehaviorSubject<StockAlert | null>(null);
  private priceUpdatesSubject$ = new BehaviorSubject<PriceUpdate[] | null>(null);
  private adminOrderAlerts$    = new BehaviorSubject<AdminOrderAlert | null>(null);
  private candidatureAlerts$   = new BehaviorSubject<CandidatureAlert | null>(null);
  private loyaltyMilestone$    = new BehaviorSubject<any | null>(null);

  // ── PUBLIC OBSERVABLES ────────────────────────────────────────────────────
  // Components subscribe to these — they are read-only views of the subjects above
  // .asObservable() prevents components from calling .next() directly
  adminOrderAlert$  = this.adminOrderAlerts$.asObservable();
  notification$     = this.notifications$.asObservable();
  stockAlert$       = this.stockAlerts$.asObservable();
  candidatureAlert$ = this.candidatureAlerts$.asObservable();
  priceUpdates$     = this.priceUpdatesSubject$.asObservable();
  milestone$        = this.loyaltyMilestone$.asObservable();

  constructor(private auth: AuthService) {}

  // ── CONNECT ───────────────────────────────────────────────────────────────
  // Called once from AppComponent after Auth0 authenticates the user
  // participantId = Auth0 sub (e.g. "google-oauth2|108378...")
  // Used to subscribe to the user's private topic: /topic/orders/{participantId}
  connect(participantId: string): void {

    // Guard: don't create multiple connections
    if (this.client?.active) return;

    // ── TRY AUTHENTICATED CONNECTION ──────────────────────────────────────
    // firstValueFrom() — same pattern as battle-websocket.service.ts
    // Waits for the token to be ready before creating the WebSocket
    // Better than .pipe(take(1)) because it's a proper async/await pattern
    firstValueFrom(this.auth.getAccessTokenSilently())
      .then(token => {

        // Token retrieved — create authenticated STOMP client
        this.client = new Client({

          // ── RAW WEBSOCKET URL ─────────────────────────────────────────
          // ws://localhost:8080/ws/websocket — same URL as battle module
          // Registered in WebSocketConfig.java:
          // registry.addEndpoint("/ws/websocket").setAllowedOriginPatterns("*")
          // Raw WebSocket — no SockJS, no CORS credential issues
          brokerURL: 'ws://localhost:8080/ws/websocket',

          // ── AUTH HEADER ──────────────────────────────────────────────
          // JWT sent as STOMP header on CONNECT frame
          // WebSocketAuthInterceptor.java reads this and validates the token
          // Sets the Principal so Spring knows WHO is connected
          connectHeaders: {
            Authorization: `Bearer ${token}`
          },

          // ── RECONNECT ────────────────────────────────────────────────
          // If connection drops, retry every 5 seconds automatically
          reconnectDelay: 5000,

          // ── HEARTBEAT DISABLED ───────────────────────────────────────
          // Same as battle module — disabling heartbeat prevents STOMP
          // timing errors during development on localhost
          heartbeatIncoming: 0,
          heartbeatOutgoing: 0,

          // ── ON CONNECT ───────────────────────────────────────────────
          // Called when STOMP handshake succeeds
          // Subscribe to all topics the user needs
          onConnect: () => {
            console.log('WebSocket connected');

            // ── ORDER STATUS NOTIFICATIONS ────────────────────────────
            // Private topic — only THIS participant receives their order updates
            // Backend: PurchaseServiceImpl.updateOrderStatus()
            // Shows toast: "Your order is on its way! 🚚"
            this.client?.subscribe(
              `/topic/orders/${participantId}`,
              (message: IMessage) => {
                const notification: OrderNotification = JSON.parse(message.body);
                this.notifications$.next(notification);
              }
            );

            // ── STOCK ALERTS ──────────────────────────────────────────
            // Public topic — ALL connected users see this
            // Backend: PurchaseServiceImpl.checkout() when stock ≤ 5
            // Shows warning: "⚠ Only 3 left of CodeArena Hoodie!"
            this.client?.subscribe(
              `/topic/stock-alert`,
              (message: IMessage) => {
                const alert: StockAlert = JSON.parse(message.body);
                this.stockAlerts$.next(alert);
              }
            );

            // ── DYNAMIC PRICE UPDATES ─────────────────────────────────
            // Public topic — all users get real-time price changes
            // Backend: DynamicPricingService every 30 seconds via @Scheduled
            // Updates prices in shop-home without page refresh
            this.client?.subscribe(
              `/topic/price-updates`,
              (message: IMessage) => {
                const updates: PriceUpdate[] = JSON.parse(message.body);
                this.priceUpdatesSubject$.next(updates);
              }
            );

            // ── ADMIN NEW ORDER ALERTS ────────────────────────────────
            // Admin topic — admin sees toast when any new order is placed
            // Backend: PurchaseServiceImpl.checkout() step 6
            // NotificationComponent checks role === 'ADMIN' before showing
            this.client?.subscribe(
              `/topic/admin/new-order`,
              (message: IMessage) => {
                const alert: AdminOrderAlert = JSON.parse(message.body);
                this.adminOrderAlerts$.next(alert);
              }
            );

            // ── ADMIN CANDIDATURE ALERTS (Event module — Nesrine) ─────
            // Admin topic — notifies admin of new event candidatures
            // Backend: event module CandidatureServiceImpl
            this.client?.subscribe(
              `/topic/admin/candidatures`,
              (message: IMessage) => {
                const alert: CandidatureAlert = JSON.parse(message.body);
                this.candidatureAlerts$.next(alert);
              }
            );

            // ── LOYALTY MILESTONE NOTIFICATIONS ──────────────────────
            // Private topic — only THIS participant receives milestone rewards
            // Backend: LoyaltyService.checkAndRewardMilestone()
            // Shows golden toast with coupon code for 10 seconds
            // Triggered when user crosses 100, 200, or 500 points
            this.client?.subscribe(
              `/topic/loyalty/${participantId}`,
              (message: IMessage) => {
                const milestone = JSON.parse(message.body);
                this.loyaltyMilestone$.next(milestone);
              }
            );
          },

          onDisconnect: () => console.log('WebSocket disconnected'),

          // Logs STOMP protocol errors — not HTTP errors
          onStompError: (frame) => console.error('STOMP error', frame)
        });

        // Start the WebSocket connection
        this.client.activate();
      })

      // ── FALLBACK: TOKEN FETCH FAILED ─────────────────────────────────
      // User not yet authenticated or Auth0 issue
      // Connect without token — public topics still work
      // Private topics (orders, loyalty) won't receive messages
      // because backend can't identify the user without JWT
      .catch(() => {
        this.client = new Client({
          // Same URL — no auth header this time
          brokerURL: 'ws://localhost:8080/ws/websocket',
          reconnectDelay: 5000,
          heartbeatIncoming: 0,
          heartbeatOutgoing: 0,

          onConnect: () => {
            console.log('WebSocket connected (no auth)');

            // Subscribe to all — public topics work, private ones don't
            this.client?.subscribe(`/topic/orders/${participantId}`,
              (message: IMessage) => {
                this.notifications$.next(JSON.parse(message.body));
              }
            );
            this.client?.subscribe(`/topic/stock-alert`,
              (message: IMessage) => {
                this.stockAlerts$.next(JSON.parse(message.body));
              }
            );
            this.client?.subscribe(`/topic/price-updates`,
              (message: IMessage) => {
                this.priceUpdatesSubject$.next(JSON.parse(message.body));
              }
            );
            this.client?.subscribe(`/topic/admin/new-order`,
              (message: IMessage) => {
                this.adminOrderAlerts$.next(JSON.parse(message.body));
              }
            );
            this.client?.subscribe(`/topic/admin/candidatures`,
              (message: IMessage) => {
                this.candidatureAlerts$.next(JSON.parse(message.body));
              }
            );
            this.client?.subscribe(`/topic/loyalty/${participantId}`,
              (message: IMessage) => {
                this.loyaltyMilestone$.next(JSON.parse(message.body));
              }
            );
          },

          onDisconnect: () => console.log('WebSocket disconnected'),
          onStompError: (frame) => console.error('STOMP error', frame)
        });

        this.client.activate();
      });
  }

  // ── DISCONNECT ────────────────────────────────────────────────────────────
  // Called manually or when component is destroyed
  // deactivate() closes the WebSocket connection gracefully
  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }

  // ── CLEANUP ───────────────────────────────────────────────────────────────
  // Angular lifecycle hook — called when the service is destroyed
  // Ensures WebSocket is closed when the app shuts down
  ngOnDestroy(): void {
    this.disconnect();
  }
}