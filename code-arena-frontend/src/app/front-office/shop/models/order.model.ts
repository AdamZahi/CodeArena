import { Product } from './product.model';

// ─── Order Status Enum ────────────────────────────────────────────
// Tracks where the order is in the pipeline
export enum OrderStatus {
  PENDING   = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  SHIPPED   = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED'
}

// ─── Order Item Interface ─────────────────────────────────────────
// One line on the receipt — one product + how many + price at purchase time
export interface OrderItem {
  id:        string;
  product:   Product;   // the product that was bought
  quantity:  number;    // how many were bought
  unitPrice: number;    // price AT THE TIME of purchase (may change later)
}

// ─── Order Interface ──────────────────────────────────────────────
// The full receipt — created when participant clicks checkout
export interface Order {
  id:            string;
  participantId: string;       // who bought it
  items:         OrderItem[];  // list of items on this receipt
  totalPrice:    number;       // sum of all items
  status:        OrderStatus;  // current status
  createdAt:     string;  
  participantName: string;     // when it was placed
}

// ─── Checkout Request ─────────────────────────────────────────────
// What Angular sends to backend when participant clicks "Acheter"
export interface CheckoutRequest {
  participantId: string;
  couponCode?: string; // ← ADD THIS
  items: {
    productId: string;
    quantity: number;
  }[];

}
