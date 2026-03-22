import { Injectable }     from '@angular/core';
import { HttpClient }     from '@angular/common/http';
import { Observable }     from 'rxjs';
import { ApiService }     from '../../../core/services/api.service';
import { ApiResponse }    from '../../../core/models/api-response.model';
import { Product }        from '../models/product.model';
import { Order }          from '../models/order.model';
import { CheckoutRequest }from '../models/order.model';
import { environment }    from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ShopService extends ApiService {

  constructor(http: HttpClient) {
    super(http, `${environment.apiBaseUrl}/api/shop`);
  }


  // ── PRODUCTS ────────────────────────────────────────────────────

  // Get all products — uses mock data until backend is ready
  getAllProducts(): Observable<ApiResponse<Product[]>> {
  return this.get<Product[]>('/products');
  }

  // Get one product by ID
  getProductById(id: string): Observable<ApiResponse<Product>> {
  return this.get<Product>(`/products/${id}`);
  }

  // Admin: create a new product
  createProduct(product: Partial<Product>): Observable<ApiResponse<Product>> {
  return this.post<Product>('/products', product);
  }

  // Admin: update a product
  updateProduct(id: string, product: Partial<Product>): Observable<ApiResponse<Product>> {
      return this.put<Product>(`/products/${id}`, product);
  }

  // Admin: delete a product
  deleteProduct(id: string): Observable<ApiResponse<void>> {
  return this.delete<void>(`/products/${id}`);
  }

  // ── ORDERS ──────────────────────────────────────────────────────

  // Participant: place an order (checkout)
  checkout(request: CheckoutRequest): Observable<ApiResponse<Order>> {
    return this.post<Order>('/orders/checkout', request);
  }

  // Participant: get my order history
// Participant: get my order history//needs to be fixed once we get user
getMyOrders(): Observable<ApiResponse<Order[]>> {
  return this.get<Order[]>('/orders/my/participant-001');
}
  // Admin: get all orders
  getAllOrders(): Observable<ApiResponse<Order[]>> {
    return this.get<Order[]>('/orders');
  }

  // Admin: update order status
updateOrderStatus(id: string, status: string): Observable<ApiResponse<Order>> {
  return this.http.put<ApiResponse<Order>>(
    `${environment.apiBaseUrl}/api/shop/orders/${id}/status?status=${status}`,
    {}
  );
}
  cancelOrder(id: string): Observable<ApiResponse<Order>> {
  return this.put<Order>(`/orders/${id}/cancel`, {});
}
getOrderQr(id: string): Observable<ApiResponse<string>> {
  return this.get<string>(`/orders/${id}/qr`);
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

}