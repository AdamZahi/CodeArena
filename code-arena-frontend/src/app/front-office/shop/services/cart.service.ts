import { Injectable }       from '@angular/core';
import { BehaviorSubject }   from 'rxjs';
import { CartItem }          from '../models/cart.model';
import { Product }           from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class CartService {

  // BehaviorSubject = reactive cart — any component subscribing 
  // will instantly see changes when cart updates
  private cartItems = new BehaviorSubject<CartItem[]>([]);

  // Public observable — components subscribe to this
  cartItems$ = this.cartItems.asObservable();

  // ── ADD TO CART ──────────────────────────────────────────────────
  addToCart(product: Product, quantity = 1): void {
    const current = this.cartItems.getValue();
    const existing = current.find(item => item.product.id === product.id);

    if (existing) {
      // product already in cart → just increase quantity
      this.cartItems.next(
        current.map(item =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + quantity }
            : item
        )
      );
    } else {
      // new product → add it
      this.cartItems.next([...current, { product, quantity }]);
    }
  }

  // ── REMOVE FROM CART ─────────────────────────────────────────────
  removeFromCart(productId: string): void {
    this.cartItems.next(
      this.cartItems.getValue().filter(item => item.product.id !== productId)
    );
  }

  // ── UPDATE QUANTITY ──────────────────────────────────────────────
  updateQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) {
      this.removeFromCart(productId);
      return;
    }
    this.cartItems.next(
      this.cartItems.getValue().map(item =>
        item.product.id === productId ? { ...item, quantity } : item
      )
    );
  }

  // ── CLEAR CART ───────────────────────────────────────────────────
  // Called after successful checkout
  clearCart(): void {
    this.cartItems.next([]);
  }

  // ── GET TOTAL ────────────────────────────────────────────────────
  // Calculates total price of all items in cart
  getTotal(): number {
    return this.cartItems.getValue().reduce(
      (total, item) => total + item.product.price * item.quantity, 0
    );
  }

  // ── GET ITEM COUNT ───────────────────────────────────────────────
  // Total number of items (for cart badge)
  getItemCount(): number {
    return this.cartItems.getValue().reduce(
      (count, item) => count + item.quantity, 0
    );
  }
}