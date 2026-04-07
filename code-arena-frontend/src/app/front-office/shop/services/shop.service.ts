import { Injectable }      from '@angular/core';
import { HttpClient }      from '@angular/common/http';
import { Observable }      from 'rxjs';
import { ApiService }      from '../../../core/services/api.service';
import { ApiResponse }     from '../../../core/models/api-response.model';
import { Product }         from '../models/product.model';
import { Order }           from '../models/order.model';
import { CheckoutRequest } from '../models/order.model';
import { environment }     from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ShopService extends ApiService {

  constructor(http: HttpClient) {
    super(http, `${environment.apiBaseUrl}/api/shop`);
  }

  // ── PRODUCTS ──────────────────────────────────

  getAllProducts(): Observable<ApiResponse<Product[]>> {
    return this.get<Product[]>('/products');
  }

  getProductById(id: string): Observable<ApiResponse<Product>> {
    return this.get<Product>(`/products/${id}`);
  }

  createProduct(product: Partial<Product>): Observable<ApiResponse<Product>> {
    return this.post<Product>('/products', product);
  }

  updateProduct(id: string, product: Partial<Product>): Observable<ApiResponse<Product>> {
    return this.put<Product>(`/products/${id}`, product);
  }

  deleteProduct(id: string): Observable<ApiResponse<void>> {
    return this.delete<void>(`/products/${id}`);
  }

  getProductsPaginated(
    page: number,
    size: number,
    sortBy: string,
    direction: string
  ): Observable<ApiResponse<any>> {
    return this.get<any>(
      `/products/paginated?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`
    );
  }

  // ── ORDERS ────────────────────────────────────

  checkout(request: CheckoutRequest): Observable<ApiResponse<Order>> {
    return this.post<Order>('/orders/checkout', request);
    // Backend now ignores request.participantId
    // and overrides it with jwt.getSubject()
    // So sending participantId in body is harmless but ignored
  }

  // ── FIX 2: Removed participantId from URL ─────
  // OLD: getMyOrders(participantId: string) → /orders/my/${participantId}
  // NEW: getMyOrders() → /orders/me
  // Backend reads participantId from JWT — cannot be spoofed
  getMyOrders(): Observable<ApiResponse<Order[]>> {
    return this.get<Order[]>('/orders/me');
  }

  getAllOrders(): Observable<ApiResponse<Order[]>> {
    return this.get<Order[]>('/orders');
  }

  updateOrderStatus(id: string, status: string): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(
      `${environment.apiBaseUrl}/api/shop/orders/${id}/status?status=${status}`,
      {}
    );
  }

  cancelOrder(id: string): Observable<ApiResponse<Order>> {
    return this.put<Order>(`/orders/${id}/cancel`, {});
    // Backend now reads participantId from JWT
    // and verifies order ownership before cancelling
  }

  getOrderQr(id: string): Observable<ApiResponse<string>> {
    return this.get<string>(`/orders/${id}/qr`);
  }

  // ── COUPON ────────────────────────────────────

  validateCoupon(code: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${environment.apiBaseUrl}/api/shop/coupons/validate`,
      { code }
    );
  }

  // ── LOYALTY POINTS ────────────────────────────

  // ── FIX 3: Removed participantId from URL ─────
  // OLD: getLoyaltyPoints(participantId: string) → /loyalty/${participantId}
  // NEW: getLoyaltyPoints() → /loyalty/me
  // Backend reads participantId from JWT — cannot read others' points
  getLoyaltyPoints(): Observable<ApiResponse<any>> {
    return this.get<any>('/loyalty/me');
  }

  // ── FIX 4: Removed participantId from body ────
  // OLD: redeemPoints(participantId, points) → body: { participantId, points }
  // NEW: redeemPoints(points) → body: { points }
  // Backend reads participantId from JWT — cannot redeem others' points
  redeemPoints(points: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${environment.apiBaseUrl}/api/shop/loyalty/redeem`,
      { points }
      // ONLY send points — participantId removed from body
      // Backend extracts it from JWT token
    );
  }

  // ── STRIPE PAYMENT ────────────────────────────

  getPaymentConfig(): Observable<ApiResponse<any>> {
    return this.get<any>('/payment/config');
  }

  createPaymentIntent(amount: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${environment.apiBaseUrl}/api/shop/payment/create-intent`,
      { amount, currency: 'usd' }
    );
  }
}