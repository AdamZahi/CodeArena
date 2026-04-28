import { Component, OnInit }  from '@angular/core';
import { CommonModule }         from '@angular/common';
import { Router }               from '@angular/router';
import { ShopService }          from '../../services/shop.service';
import { Order, OrderStatus }   from '../../models/order.model';
import { AuthService }          from '@auth0/auth0-angular';
import { take }                 from 'rxjs/operators';

@Component({
  selector: 'app-my-inventory',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-inventory.component.html',
  styleUrl: './my-inventory.component.css'
})
export class MyInventoryComponent implements OnInit {

  orders: Order[] = [];
  isLoading = true;
  selectedOrder: Order | null = null;
  participantId = '';
  orderQrCodes: { [orderId: string]: string } = {};

  // ── LOYALTY POINTS ────────────────────────────
  loyaltyPoints = 0;
  redeemableValue = 0;
  canRedeem = false;
  loyaltyLoading = true;

  constructor(
    private shopService: ShopService,
    private router: Router,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
    this.loadLoyaltyPoints();
  }

  loadOrders(): void {
    this.isLoading = true;
    this.shopService.getMyOrders().subscribe({
      next: (res) => {
        this.orders = res.data || [];
        this.isLoading = false;
      },
      error: () => {
        this.orders = [];
        this.isLoading = false;
      }
    });
  }

  // ── LOAD LOYALTY POINTS ───────────────────────
  // Calls GET /api/shop/loyalty/me
  // Backend reads identity from JWT — secure
  loadLoyaltyPoints(): void {
    this.shopService.getLoyaltyPoints().subscribe({
      next: (res) => {
        this.loyaltyPoints    = res.data?.points || 0;
        this.redeemableValue  = res.data?.redeemableValue || 0;
        this.canRedeem        = res.data?.canRedeem || false;
        this.loyaltyLoading   = false;
      },
      error: () => {
        this.loyaltyLoading = false;
      }
    });
  }

  // ── LOYALTY LEVEL LABEL ───────────────────────
  getLoyaltyLevel(): { label: string; color: string; emoji: string } {
    if (this.loyaltyPoints >= 500) return { label: 'PLATINUM', color: '#06b6d4', emoji: '💎' };
    if (this.loyaltyPoints >= 200) return { label: 'GOLD',     color: '#f59e0b', emoji: '🥇' };
    if (this.loyaltyPoints >= 100) return { label: 'SILVER',   color: '#94a3b8', emoji: '🥈' };
    return                                { label: 'BRONZE',   color: '#b45309', emoji: '🥉' };
  }

  // ── PROGRESS TO NEXT LEVEL ────────────────────
getLoyaltyProgress(): number {
  if (this.loyaltyPoints >= 500) return 100;
  if (this.loyaltyPoints >= 200) {
    return Math.min(100, ((this.loyaltyPoints - 200) / 300) * 100);
  }
  if (this.loyaltyPoints >= 100) {
    return Math.min(100, ((this.loyaltyPoints - 100) / 100) * 100);
  }
  return Math.min(100, (this.loyaltyPoints / 100) * 100);
}

getNextLevelPoints(): number {
  if (this.loyaltyPoints >= 500) return this.loyaltyPoints; // already max
  if (this.loyaltyPoints >= 200) return 500;
  if (this.loyaltyPoints >= 100) return 200;
  return 100;
}

  viewOrder(order: Order): void {
    this.selectedOrder = order;
    this.loadQrCode(order.id);
  }

  closeModal(): void {
    this.selectedOrder = null;
  }

  goToShop(): void {
    this.router.navigate(['/shop']);
  }

  getStatusColor(status: OrderStatus): string {
    switch(status) {
      case OrderStatus.PENDING:   return 'status-pending';
      case OrderStatus.CONFIRMED: return 'status-confirmed';
      case OrderStatus.SHIPPED:   return 'status-shipped';
      case OrderStatus.DELIVERED: return 'status-delivered';
      case OrderStatus.CANCELLED: return 'status-cancelled';
      default: return '';
    }
  }

  getItemTotal(price: number, qty: number): number {
    return price * qty;
  }

  cancelOrder(orderId: string, event: Event): void {
    event.stopPropagation();
    this.shopService.cancelOrder(orderId).subscribe({
      next: () => {
        const order = this.orders.find(o => o.id === orderId);
        if (order) order.status = OrderStatus.CANCELLED;
      },
      error: (err) => console.error('Cancel failed', err)
    });
  }

  loadQrCode(orderId: string): void {
    if (this.orderQrCodes[orderId]) return;
    this.shopService.getOrderQr(orderId).subscribe({
      next: (res) => {
        this.orderQrCodes[orderId] = res.data;
      }
    });
  }

  // ── ECO IMPACT ────────────────────────────────
get ecoFriendlyOrders(): number {
  return this.orders.filter(o => o.status !== 'CANCELLED').length;
}

get totalSpent(): number {
  return this.orders
    .filter(o => o.status !== 'CANCELLED')
    .reduce((sum, o) => sum + o.totalPrice, 0);
}

get ecoRating(): { label: string; color: string; emoji: string } {
  const ratio = this.orders.length > 0
    ? this.ecoFriendlyOrders / this.orders.length
    : 0;
  if (ratio >= 0.7) return { label: 'Excellent', color: '#10b981', emoji: '🌱' };
  if (ratio >= 0.5) return { label: 'Good',      color: '#22c55e', emoji: '🌿' };
  if (ratio >= 0.3) return { label: 'Average',   color: '#f59e0b', emoji: '♻️' };
  return               { label: 'Poor',      color: '#ef4444', emoji: '⚠️' };
}

get carbonSaved(): number {
  // Estimate: each eco-friendly order saves ~0.3kg CO2 vs non-eco
  return Math.round(this.ecoFriendlyOrders * 0.3 * 10) / 10;
}

get ecoPercentage(): number {
  if (this.orders.length === 0) return 0;
  const validOrders = this.orders.filter(o => o.status !== 'CANCELLED').length;
  return Math.round((validOrders / this.orders.length) * 100);
}
}