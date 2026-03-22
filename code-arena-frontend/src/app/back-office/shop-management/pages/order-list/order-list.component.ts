import { Component, OnInit }  from '@angular/core';
import { CommonModule }         from '@angular/common';
import { FormsModule }          from '@angular/forms';
import { Router }               from '@angular/router';
import { ShopService }          from '../../../../front-office/shop/services/shop.service';
import { Order, OrderStatus }   from '../../../../front-office/shop/models/order.model';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {

  orders: Order[] = [];
  filteredOrders: Order[] = [];
  isLoading = true;
  selectedStatus: string = '';
  selectedOrder: Order | null = null;

  // All statuses for filter dropdown
  statuses = Object.values(OrderStatus);

  // Next status map — what comes after each status
  nextStatus: Record<string, OrderStatus> = {
    'PENDING':   OrderStatus.CONFIRMED,
    'CONFIRMED': OrderStatus.SHIPPED,
    'SHIPPED':   OrderStatus.DELIVERED
  };

  constructor(
    private shopService: ShopService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading = true;
    this.shopService.getAllOrders().subscribe({
      next: (res) => {
        this.orders = res.data || [];
        this.filteredOrders = [...this.orders];
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  // Filter by status
  filterByStatus(): void {
    if (!this.selectedStatus) {
      this.filteredOrders = [...this.orders];
    } else {
      this.filteredOrders = this.orders.filter(
        o => o.status === this.selectedStatus
      );
    }
  }

  // View order detail
  viewOrder(order: Order): void {
    this.selectedOrder = order;
    this.loadQrCode(order.id);
  }

  closeModal(): void {
    this.selectedOrder = null;
  }

  // Advance order to next status
  advanceStatus(order: Order, event: Event): void {
    event.stopPropagation();
    const next = this.nextStatus[order.status];
    if (!next) return;

    this.shopService.updateOrderStatus(order.id, next).subscribe({
      next: () => {
        order.status = next;
        if (this.selectedOrder?.id === order.id) {
          this.selectedOrder.status = next;
        }
      },
      error: (err) => console.error('Update failed', err)
    });
  }

  // Cancel order
  cancelOrder(order: Order, event: Event): void {
    event.stopPropagation();
    this.shopService.cancelOrder(order.id).subscribe({
      next: () => {
        order.status = OrderStatus.CANCELLED;
        if (this.selectedOrder?.id === order.id) {
          this.selectedOrder.status = OrderStatus.CANCELLED;
        }
      },
      error: (err) => console.error('Cancel failed', err)
    });
  }

  // Can this order be advanced?
  canAdvance(status: string): boolean {
    return ['PENDING', 'CONFIRMED', 'SHIPPED'].includes(status);
  }

  // Status color
  getStatusColor(status: string): string {
    const map: Record<string, string> = {
      'PENDING':   'status-pending',
      'CONFIRMED': 'status-confirmed',
      'SHIPPED':   'status-shipped',
      'DELIVERED': 'status-delivered',
      'CANCELLED': 'status-cancelled'
    };
    return map[status] || '';
  }

  // Next status label
  getNextLabel(status: string): string {
    const map: Record<string, string> = {
      'PENDING':   '→ CONFIRM',
      'CONFIRMED': '→ SHIP',
      'SHIPPED':   '→ DELIVER'
    };
    return map[status] || '';
  }

  goToProducts(): void {
    this.router.navigate(['/admin/shop']);
  }

  // Count orders by status
  countByStatus(status: string): number {
    return this.orders.filter(o => o.status === status).length;
  }
  // Add property:
orderQrCodes: { [orderId: string]: string } = {};

// Add method:
loadQrCode(orderId: string): void {
  if (this.orderQrCodes[orderId]) return;
  this.shopService.getOrderQr(orderId).subscribe({
    next: (res) => {
      this.orderQrCodes[orderId] = res.data;
    }
  });
}
exportOrders(): void {
  window.open(`${environment.apiBaseUrl}/api/shop/orders/export`, '_blank');
}
}