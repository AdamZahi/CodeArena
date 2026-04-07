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

  constructor(
    private shopService: ShopService,
    private router: Router,
    private auth: AuthService
  ) {}

ngOnInit(): void {
  this.loadOrders();
  // No need to wait for user.sub — backend reads from JWT automatically
}

loadOrders(): void {
  this.isLoading = true;
  this.shopService.getMyOrders().subscribe({
    // No participantId argument — removed
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


  // Open order detail modal + load QR
  viewOrder(order: Order): void {
    this.selectedOrder = order;
    this.loadQrCode(order.id);
  }

  closeModal(): void {
    this.selectedOrder = null;
  }

  // Go back to shop
  goToShop(): void {
    this.router.navigate(['/shop']);
  }

  // Status color helper
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

  // Calculate order item total
  getItemTotal(price: number, qty: number): number {
    return price * qty;
  }

  // Cancel order + restore stock
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

  // Load QR code for order
  loadQrCode(orderId: string): void {
    if (this.orderQrCodes[orderId]) return;
    this.shopService.getOrderQr(orderId).subscribe({
      next: (res) => {
        this.orderQrCodes[orderId] = res.data;
      }
    });
  }
}